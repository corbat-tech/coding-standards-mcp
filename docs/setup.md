# Setup Guide

Complete setup instructions for all supported tools and IDEs.

---

## Table of Contents

- [IDEs & Editors](#ides--editors)
  - [Cursor](#cursor)
  - [VS Code](#vs-code)
  - [Windsurf](#windsurf)
  - [JetBrains IDEs](#jetbrains-ides)
  - [Zed](#zed)
  - [Claude Desktop](#claude-desktop)
  - [Claude Code (CLI)](#claude-code-cli)
  - [Eclipse](#eclipse)
  - [Neovim](#neovim)
  - [Replit](#replit)
- [AI Extensions](#ai-extensions)
  - [GitHub Copilot](#github-copilot)
  - [Continue](#continue)
  - [Cline](#cline)
  - [Other Extensions](#other-extensions)
- [Troubleshooting](#troubleshooting)

---

## IDEs & Editors

### Cursor

Add to `.cursor/mcp.json` in your project root:

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

Restart Cursor after adding the configuration.

---

### VS Code

Add to `.vscode/mcp.json` in your project root:

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

This works with GitHub Copilot, Continue, Cline, and other MCP-compatible extensions.

---

### Windsurf

Add to `~/.codeium/windsurf/mcp_config.json`:

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

Restart Windsurf after adding the configuration.

---

### JetBrains IDEs

Works with IntelliJ IDEA, PyCharm, WebStorm, Android Studio, GoLand, Rider, PhpStorm, RubyMine, CLion, and DataGrip.

1. Go to **Settings → Tools → AI Assistant → Model Context Protocol**
2. Add a new MCP server with this configuration:

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

### Zed

Add to `~/.config/zed/settings.json`:

```json
{
  "context_servers": {
    "corbat": {
      "command": {
        "path": "npx",
        "args": ["-y", "@corbat-tech/coding-standards-mcp"]
      }
    }
  }
}
```

---

### Claude Desktop

Edit `~/.config/Claude/claude_desktop_config.json`:

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

Restart Claude Desktop after adding the configuration.

---

### Claude Code (CLI)

Run this command:

```bash
claude mcp add corbat -- npx -y @corbat-tech/coding-standards-mcp
```

That's it! The MCP is now available in your Claude Code sessions.

---

### Eclipse

1. Go to **Window → Preferences → AI Assistant → MCP Servers**
2. Add a new server with the command: `npx -y @corbat-tech/coding-standards-mcp`

---

### Neovim

Using [mcphub.nvim](https://github.com/ravitemer/mcphub.nvim), add to your Neovim config:

```lua
require('mcphub').setup({
  servers = {
    corbat = {
      command = "npx",
      args = { "-y", "@corbat-tech/coding-standards-mcp" }
    }
  }
})
```

---

### Replit

In your Replit project:
1. Go to **Settings → AI**
2. Add MCP server configuration with the command: `npx -y @corbat-tech/coding-standards-mcp`

---

## AI Extensions

### GitHub Copilot

GitHub Copilot uses your IDE's MCP configuration. Set up MCP in your IDE (VS Code, JetBrains, etc.) and Copilot will use it automatically.

---

### Continue

Continue uses VS Code's MCP configuration (`.vscode/mcp.json`). Follow the [VS Code setup](#vs-code) instructions.

---

### Cline

Cline uses VS Code's MCP configuration (`.vscode/mcp.json`). Follow the [VS Code setup](#vs-code) instructions.

---

### Other Extensions

These extensions also support MCP through your IDE's configuration:

| Extension | IDE | Setup |
|-----------|-----|-------|
| Sourcegraph Cody | VS Code, JetBrains | Use IDE's MCP config |
| Tabnine | VS Code, JetBrains, Neovim | Use IDE's MCP config |
| Amazon Q | VS Code, JetBrains | Use IDE's MCP config |
| Google Gemini Code Assist | VS Code, JetBrains | Use IDE's MCP config |
| Refact.ai | VS Code, JetBrains | Use IDE's MCP config |
| Codium AI (Qodo) | VS Code, JetBrains | Use IDE's MCP config |

---

## Troubleshooting

### AI can't find corbat

1. Verify npm/npx is in PATH: `which npx`
2. Test manually: `npx @corbat-tech/coding-standards-mcp`
3. Restart your IDE/editor completely
4. Check MCP logs in your tool's settings

### Wrong stack detected

Override with `.corbat.json` in your project root:

```json
{ "profile": "nodejs" }
```

Or specify in your prompt: *"...using profile nodejs"*

### Permission errors (macOS/Linux)

```bash
npx clear-npx-cache
npx @corbat-tech/coding-standards-mcp
```

---

[Back to README](../README.md) · [Full Documentation](full-documentation.md) · [Compatibility Matrix](compatibility.md)
