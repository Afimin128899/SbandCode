# Model suggestions from skills

In a developer build, a skill installed under the app's skills directory can
suggest an already configured provider model. Add a `model` field to the front
matter of its `SKILL.md`.

```yaml
---
name: review-kotlin
description: Review Kotlin changes
model: openai/gpt-5.1
---
```

Open **Tools → Skills and MCP** and tap the model beside the skill. The selection
uses the normal model picker logic and applies to the active chat. A skill can
suggest an available model by its provider and model ID. Only the user's tap
changes the selection.
