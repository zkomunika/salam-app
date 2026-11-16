#!/usr/bin/env python3
"""Export a local HTML file or URL to a PNG image at exact dimensions using Playwright."""

import argparse
import sys
from pathlib import Path


def _resolve_target_url(html_arg: str) -> str:
    lowered = html_arg.strip().lower()
    if lowered.startswith(("http://", "https://", "file://")):
        return html_arg.strip()
    path = Path(html_arg).expanduser().resolve()
    if not path.exists():
        raise FileNotFoundError(f"HTML file not found: {path}")
    return path.as_uri()


def export_png(html: str, width: int, height: int, output: str, scale: float = 2.0) -> Path:
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        print(
            "Playwright is not installed. Install it first:\n"
            "pip install playwright && python -m playwright install chromium",
            file=sys.stderr,
        )
        raise SystemExit(1)

    target_url = _resolve_target_url(html)
    out_path = Path(output).expanduser().resolve()
    out_path.parent.mkdir(parents=True, exist_ok=True)

    with sync_playwright() as p:
        browser = None
        last_exc = None
        for launch_kwargs in ({}, {"channel": "chrome"}, {"channel": "msedge"}):
            try:
                browser = p.chromium.launch(**launch_kwargs)
                break
            except Exception as exc:
                last_exc = exc
        if browser is None:
            print(
                f"Failed to launch Chromium ({last_exc}). Ensure Playwright and Chromium are installed:\n"
                "pip install playwright && python -m playwright install chromium",
                file=sys.stderr,
            )
            raise SystemExit(1)
        try:
            page = browser.new_page(
                viewport={"width": width, "height": height},
                device_scale_factor=scale,
            )
            page.goto(target_url, wait_until="networkidle")
            page.evaluate("() => document.fonts.ready")
            page.screenshot(path=str(out_path), type="png")
        finally:
            browser.close()

    return out_path


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Render an HTML banner or social photo to PNG at exact viewport dimensions.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=(
            "Examples:\n"
            "  python .agents/skills/banner-design/scripts/export-png.py \\\n"
            "    --html assets/banners/hero.html --width 1500 --height 500 \\\n"
            "    --output assets/banners/hero-1500x500.png\n"
            "  python .agents/skills/banner-design/scripts/export-png.py \\\n"
            "    --html http://localhost:8765/banner.html --width 1080 --height 1080 \\\n"
            "    --output out.png --scale 1\n"
        ),
    )
    parser.add_argument(
        "--html",
        required=True,
        help="Path to local HTML file or HTTP/file URL to render.",
    )
    parser.add_argument(
        "--width",
        type=int,
        required=True,
        help="Viewport width in CSS pixels (e.g. 1500).",
    )
    parser.add_argument(
        "--height",
        type=int,
        required=True,
        help="Viewport height in CSS pixels (e.g. 500).",
    )
    parser.add_argument(
        "--output",
        required=True,
        help="Destination PNG file path (parent directories are created automatically).",
    )
    parser.add_argument(
        "--scale",
        type=float,
        default=2.0,
        help="Device scale factor for crisp high-DPI export (default: 2).",
    )

    args = parser.parse_args()
    out_path = export_png(
        html=args.html,
        width=args.width,
        height=args.height,
        output=args.output,
        scale=args.scale,
    )
    print(f"Exported PNG: {out_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
