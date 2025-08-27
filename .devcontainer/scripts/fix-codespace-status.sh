#!/usr/bin/env bash
set -euo pipefail

# Fix Codespaces terminal status tool invocation by ensuring a VS Code Server node path exists.
# Some environments launch the status tool via:
#   $(ls ~/.vscode-remote/bin/*/node | head -n 1) /workspaces/.codespaces/shared/codespaceStatusTool.js
# If the server isn't downloaded yet, the glob is empty and the command fails.

if [ "${CODESPACES:-false}" = "true" ]; then
  VSCODE_BIN_BASE="${HOME}/.vscode-remote/bin"
  mkdir -p "${VSCODE_BIN_BASE}/placeholder"
  if [ ! -x "${VSCODE_BIN_BASE}/placeholder/node" ]; then
    NODE_PATH="$(command -v node || true)"
    if [ -n "${NODE_PATH}" ]; then
      ln -sf "${NODE_PATH}" "${VSCODE_BIN_BASE}/placeholder/node"
      echo "[postCreate] Linked placeholder VS Code Server node at ${VSCODE_BIN_BASE}/placeholder/node"
    else
      echo "[postCreate][WARN] Node not found on PATH; cannot set placeholder node for Codespaces status tool."
    fi
  fi
fi

echo "[postCreate] Codespaces status tool placeholder setup completed."

