# 贡献指南

[English](CONTRIBUTING.md) | [简体中文](CONTRIBUTING_zh-CN.md)

> [!IMPORTANT]
> 本项目是 [Denizen](https://github.com/DenizenScript/Denizen) 的非官方 Fork，为 MINEZ 服务器而维护，与 DenizenScript 团队没有任何关联，亦未获得其背书或支持。
>
> 请勿将与本 Fork 有关的任何内容带到上游的 issue、Discord 或论坛。

## 提交 issue 之前

请先确认该问题在未经修改的上游构建上是否同样出现。

- **上游同样出现** —— 那是上游的问题，请通过[他们的渠道](https://discord.gg/Q6pZGSR)反馈，而不是这里；反馈时也请不要提及本 Fork。
- **只在这里出现** —— 请提交至[本仓库的 issue](https://github.com/MINEZ/Denizen/issues)。

若问题与本 Fork 新增的功能有关，请先查阅[文档站](https://denizen-meta.minez.cc/)。

## 关于 Pull Request

本 Fork 只为一个服务器服务，范围是刻意收窄的。与之无关的改动，无论多好都不太可能被合并——那类内容更适合提给上游，也能惠及更多人。

如果你确实要提交 Pull Request：

- 只提交你拥有版权、或可自由使用的代码。若其中任何部分受既有许可证约束，请在 PR 中说明。
- 与周围代码的风格保持一致。大体是标准 Java 约定，有一处沿袭自上游的显著差异：`}` 之后总要空一行。
- 针对改动可能影响的游戏版本进行测试，涉及 NMS 的尤其如此。
- 说明这个改动解决了什么问题，而不只是它做了什么。

## 许可

本项目采用 MIT 许可证。提交贡献即表示你同意你的贡献同样适用该许可证。

本项目没有贡献者许可协议（CLA）。上游仓库中的那一份仅对上游有效，与本 Fork 无关。
