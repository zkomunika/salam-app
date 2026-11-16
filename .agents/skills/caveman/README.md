# caveman

Talk like smart caveman. Same brain, fewer tokens.

## What it does

Makes the model answer first and cut the ceremony: no greeting, hedging,
filler, recap, or closer. Articles drop when the sentence still reads in one
pass. Code, commands, paths, numbers, and exact error strings never change.
Result depends on model and workload. Measured once on the 3.1.0 skill text (`evals/snapshots/results.json`: claude-opus-5-5, ten prompts, single run, output length only, tiktoken o200k approximation): caveman 3% fewer output tokens at the median than a plain `Answer concisely.` control, inside the noise; ultracave 35%; megacave 9%. No quality-equivalence claim is published. Mode persists until changed or stopped.

## How to invoke

```
/caveman              # the voice (default)
/caveman status       # show current mode without changing it
stop caveman          # back to normal prose
```

## Example output

Question: "Why does my React component re-render?"

Normal prose:
> Your component re-renders because you create a new object reference each render. Wrapping it in `useMemo` will fix the issue.

caveman:
> New object ref each render, so React re-renders. Wrap the prop in `useMemo`.

## See also

- [`SKILL.md`](./SKILL.md): full LLM-facing instructions
