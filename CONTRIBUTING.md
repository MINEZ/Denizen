# Contributing

[English](CONTRIBUTING.md) | [简体中文](CONTRIBUTING_zh-CN.md)

> [!IMPORTANT]
> This is an unofficial fork of [Denizen](https://github.com/DenizenScript/Denizen), maintained for the MINEZ server. It is not affiliated with, endorsed by, or supported by the DenizenScript team.
>
> Please do not bring anything about this fork to upstream's issue tracker, Discord, or forums.

## Before Opening an Issue

Check whether the problem also happens on an unmodified upstream build.

- **It still happens without this fork** — an upstream problem. Report it through [their channels](https://discord.gg/Q6pZGSR) as you normally would, with no mention of this fork.
- **It does not happen without this fork** — ours. It belongs in [this repository's issue tracker](https://github.com/MINEZ/Denizen/issues), and only there.

For anything specific to the features this fork adds, see the [documentation site](https://denizen-meta.minez.cc/) first.

## Pull Requests

This fork exists to serve one server, so its scope is intentionally narrow. Changes unrelated to that are unlikely to be merged, however good they are — those are better proposed upstream, where they will reach far more people.

If you do open a pull request:

- Only submit code you own or that is free to use. If any part of it comes under a pre-existing license, say so in the pull request.
- Match the surrounding code style. It is largely standard Java conventions, with one notable difference inherited from upstream: there is always a newline after `}`.
- Test against the game versions your change could affect, especially anything touching NMS.
- Explain what problem the change solves, not just what it does.

## Licensing

This project is under the MIT License. By contributing, you agree your contribution is provided under the same terms.

There is no contributor license agreement. The one in upstream's repository applies to upstream only and has nothing to do with this fork.
