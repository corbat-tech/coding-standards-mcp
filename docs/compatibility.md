# Compatibility Matrix

Complete list of all tools, IDEs, and extensions that support Corbat MCP.

---

## IDEs & Editors

| IDE/Editor | Status | Documentation |
|------------|:------:|---------------|
| Cursor | ✅ Tested | [Setup](setup.md#cursor) |
| VS Code | ✅ Tested | [Setup](setup.md#vs-code) |
| Windsurf | ✅ Tested | [Setup](setup.md#windsurf) |
| JetBrains IDEs | ✅ Tested | [Setup](setup.md#jetbrains-ides) |
| Zed Editor | ✅ Tested | [Setup](setup.md#zed) |
| Eclipse | ✅ Tested | [Setup](setup.md#eclipse) |
| Neovim | ✅ Tested | [Setup](setup.md#neovim) |
| Theia IDE | ✅ Tested | v1.57+ |
| Replit | ✅ Tested | [Setup](setup.md#replit) |

**JetBrains IDEs includes:** IntelliJ IDEA, PyCharm, WebStorm, Android Studio, GoLand, Rider, PhpStorm, RubyMine, CLion, DataGrip

---

## AI Extensions & Plugins

| Extension | Compatible IDEs | Status |
|-----------|-----------------|:------:|
| GitHub Copilot | VS Code, JetBrains, Eclipse, Xcode, Neovim | ✅ Tested |
| Continue | VS Code, JetBrains | ✅ Tested |
| Cline | VS Code | ✅ Tested |
| Sourcegraph Cody | VS Code, JetBrains | ✅ Tested |
| Tabnine | VS Code, JetBrains, Neovim | ✅ Tested |
| Amazon Q | VS Code, JetBrains | ✅ Tested |
| Google Gemini Code Assist | VS Code, JetBrains | ✅ Tested |
| Refact.ai | VS Code, JetBrains | ✅ Tested |
| Codium AI (Qodo) | VS Code, JetBrains | ✅ Tested |

---

## AI Agents & Desktop Apps

| Tool | Status | Notes |
|------|:------:|-------|
| Claude Desktop | ✅ Tested | Desktop Extensions support |
| Claude Code (CLI) | ✅ Tested | Official Anthropic CLI |
| ChatGPT | ✅ Tested | Developer Mode (remote servers only) |
| Devin | ✅ Tested | MCP Marketplace integrated |
| OpenHands | ✅ Tested | Open source agent (formerly OpenDevin) |
| SWE-agent | ✅ Tested | Active development |
| Sweep | ✅ Tested | MCP servers integrated |
| Aider | ⚠️ Partial | Community servers available |

---

## Web Platforms

| Platform | Status | Notes |
|----------|:------:|-------|
| Lovable | ✅ Tested | Personal connectors |
| Replit Ghostwriter | ✅ Tested | MCP integrated |
| Vercel v0 | ⚠️ Partial | Context only, not generated code |
| Bolt.new | 🔜 Planned | Roadmap summer 2026 |

---

## Summary

| Category | Tested | Total |
|----------|:------:|:-----:|
| IDEs & Editors | 9 | 9 |
| AI Extensions | 9 | 9 |
| AI Agents & Apps | 7 | 8 |
| Web Platforms | 2 | 4 |
| **Total** | **27** | **30** |

---

## Standard MCP Protocol

Corbat MCP uses the standard [Model Context Protocol](https://modelcontextprotocol.io/), which means it's compatible with **any tool that supports MCP**.

If your tool supports MCP but isn't listed here, it should work. The basic configuration is:

```json
{
  "mcpServers": {
    "corbat": {
      "command": "npx",
      "args": ["-y", "@corbat-tech/coding-standards-mcp"]
    }
  }
}
```

---

## Official Documentation Links

| Tool | MCP Documentation |
|------|-------------------|
| Cursor | [cursor.com/docs/context/mcp](https://cursor.com/docs/context/mcp) |
| VS Code | [code.visualstudio.com/docs/copilot/mcp-servers](https://code.visualstudio.com/docs/copilot/customization/mcp-servers) |
| Windsurf | [docs.windsurf.com/cascade/mcp](https://docs.windsurf.com/windsurf/cascade/mcp) |
| JetBrains | [jetbrains.com/help/ai-assistant/mcp](https://www.jetbrains.com/help/ai-assistant/mcp.html) |
| Zed | [zed.dev/docs/assistant/model-context-protocol](https://zed.dev/docs/assistant/model-context-protocol) |
| Eclipse | [eclipse.dev/lmos/docs/arc/mcp](https://eclipse.dev/lmos/docs/arc/mcp/) |
| Replit | [docs.replit.com/replitai/mcp](https://docs.replit.com/replitai/mcp/overview) |
| Claude Desktop | [support.claude.com](https://support.claude.com/en/articles/10949351-getting-started-with-local-mcp-servers-on-claude-desktop) |

---

[Back to README](../README.md) · [Setup Guide](setup.md) · [Full Documentation](full-documentation.md)
