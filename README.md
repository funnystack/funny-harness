# funny-harness

英文文档|[中文文档](README_zh.md)

Engineering infrastructure for AI Agent teams. Standardize your team environment in 5 minutes.

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version 2.0.0](https://img.shields.io/badge/version-2.0.0-green.svg)](version.json)

## What is funny-harness

funny-harness is an AI Agent engineering infrastructure project by [funny2048](https://github.com/funny2048). It provides a one-click installer that sets up team-wide coding standards, security guards, skills, and self-evolution mechanisms for Claude Code. The goal: make every AI session start from a known-good baseline, not from zero.

The core insight is that **environment design pays off far more than prompt tuning**. A well-harnessed average model produces reliable code; an unharnessed top model still falls into the same traps repeatedly.

## Why funny-harness

Most teams invest heavily in prompt engineering but neglect the environment the AI operates in. funny-harness takes the opposite approach: hard constraints over soft reminders, state machines over suggestions, and persistent memory over one-shot context.

Three design pillars:

- **Readability** -- AGENTS.md as a navigation map, progressive disclosure via docs/, OpenSpec specs as living documents
- **Defense** -- Hooks and permissions as hard constraints, state machine for execution phases, pre-write validation, failure loop detection (auto-stop after 3 retries)
- **Feedback Loop** -- Separate agents for implementation and review, persistent error memory, Critic-to-Refiner evolution cycle, repeatable patterns compiled into scripts

## Quick Start

```bash
git clone https://github.com/funny2048/funny-harness.git
cd funny-harness
chmod +x install.sh
./install.sh
```

The installer walks you through:

1. Enter your name
2. Copy team-level CLAUDE.md to system directory
3. Copy user-level CLAUDE.md to ~/.claude/ (replaces placeholder with your name)
4. Install hooks (merge JSON into ~/.claude/settings.json)
5. Select language rules (golang/java/python/typescript, multi-select)
6. Install rules to ~/.claude/rules/ (common is mandatory plus selected languages)
7. Install skills to ~/.claude/skills/

Requirements: bash, jq (for hook merging).

## Project Structure

```
funny-harness/
  install.sh              # One-click installer
  AGENTS.md               # Agent navigation map
  LICENSE                 # MIT
  version.json            # Current version metadata
  harness-design.md       # Design philosophy document
  agents/                 # AI agent definitions
    architect.md
    executor.md
    reviewer.md
  hooks/                  # Safety guards
    check-dangerous.json
    protect-files.json
    ensure_change_context.py
    prettier-lint.json
    run-test.json
    script/
      check-dangerous.sh
      protect-files.sh
  openspec/               # OpenSpec workflow
    config.yaml
    changes/
      archive/
    specs/
  rules/                  # Coding rules
    common/
    golang/
    java/
    python/
    typescript/
  scripts/                # Validation scripts
    check-docs.sh
    lint-deps.sh
    lint-quality.sh
    validate.sh
  skills/                 # Specialized skills
    harness-init-java/
    review-summary/
    spring-architecture-review/
    sql-risk-review/
    openspec-bridge/
  templates/              # File templates
    CLAUDE-team-template.md
    CLAUDE-user-template.md
    exec-plan-template.md
    openspec-config.yaml
    settings.local.json.example
    skill-bundle.json
```

## Core Components

### Skills

| Skill | Trigger | Description |
|-------|---------|-------------|
| harness-init-java | `/harness-init-java` | Scans Java project and generates AGENTS.md + CLAUDE.md + docs/ knowledge base + harness/ self-evolution directory |
| review-summary | `/review-summary` | Collects git changes and generates structured review reports from REVIEW.md checklist |
| spring-architecture-review | `/spring-architecture-review` | Audits Controller/Service/DAO layering compliance in Spring Boot projects |
| sql-risk-review | `/sql-risk-review` | Detects missing WHERE clauses, SELECT *, cross-database joins, and index risks |
| openspec-bridge | `/openspec-bridge` | Bridges OpenSpec config.yaml into AI context, preventing chain-of-knowledge breaks |

### Agents

| Agent | Trigger | Description |
|-------|---------|-------------|
| reviewer | `@reviewer` | Independent read-only code reviewer |
| architect | `@architect` | Architecture-level reviewer (layering, data flow, interface design, dependency management, extensibility) |
| executor | `@executor` | Executes implementation from tasks.md, strictly scoped to task boundaries |

### Hooks

| Hook | Type | Trigger | Description |
|------|------|---------|-------------|
| check-dangerous | PreToolUse | Bash | Blocks dangerous operations (recursive delete, table drops, force push, etc.) |
| protect-files | PreToolUse | Write/Edit | Blocks writes to sensitive files: .env, .git/, *.pem, *.key, secrets/ |
| ensure_change_context | PreToolUse | Bash | Warns when high-risk commands run without an active OpenSpec change |
| prettier-lint | PostToolUse | Write/Edit | Auto-formats code with Prettier and ESLint after edits |
| run-test | PostToolUse | Write/Edit | Runs tests (npm test / mvn test) after file modifications |

### Rules

Common rules (always installed) cover: agents, coding-style, development-workflow, git-workflow, hooks, patterns, performance, security, testing.

Language-specific rules are available for: golang, java, python, typescript. Each extends common rules with language conventions, formatting tools, and framework-specific patterns.

## OpenSpec Workflow

OpenSpec manages the full lifecycle of a change, from proposal to archive:

```
1. /opsx:propose    Requirement becomes artifacts (proposal, design, tasks)
       |
2. Human review     Approve scope and approach
       |
3. /opsx:apply      Execute implementation per tasks.md
       |
4. Specialized review
   - review-summary
   - spring-architecture-review
   - sql-risk-review
       |
5. /opsx:verify     Check implementation matches artifacts
       |
6. /opsx:archive    Archive change, update specs/
```

No direct development outside the change workflow. High-risk operations require an active change context.

## Self-Evolution

### Three-Layer Memory

Located in the `harness/` directory (generated per project by harness-init-java):

- **Episodic memory** (episodic.md) -- Specific events and lessons learned
- **Procedural memory** (procedural.md) -- Successful operation step patterns
- **Failure experience** (lessons-learned.md) -- Patterns for the Critic agent to analyze

### Trajectory Compilation

When the same task type succeeds 3+ times with highly consistent steps, the pattern is compiled into a deterministic script. This is a ratchet effect: every compiled pattern becomes permanent infrastructure that prevents regression.

The evolution cycle:

```
Agent executes -> Verification catches issues -> Critic analyzes patterns
-> Refiner updates rules -> Next agent benefits
```

## Available Commands

| Command | Description |
|---------|-------------|
| `bash scripts/check-docs.sh` | Document consistency check |
| `bash scripts/validate.sh` | Unified validation pipeline (build, lint-arch, test, verify) |
| `bash scripts/lint-deps.sh` | Dependency direction check |
| `bash scripts/lint-quality.sh` | Code quality check |

## Contributing

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

Keep changes minimal and scoped.

## License

[MIT License](LICENSE) (c) 2026 funny2048

