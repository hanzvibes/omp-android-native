# Remotion Specialist Mode — Product & Technical Specification

**Status:** Approved product direction; implementation not yet device-verified.
**Target:** OMP Android Native
**Mode:** Specialized Remotion agent
**Execution:** Hybrid (Android authoring, cloud rendering)
**Preview:** Embedded in OMP

## Product outcome

Turn OMP into an Android-first Remotion authoring environment. The user can ask the specialist agent to create and edit a Remotion project, inspect a composition in an embedded preview, and submit final renders to a cloud worker. Android is the authoring/control surface; it is not the production renderer.

This feature must not block or destabilize the existing OMP terminal/runtime work.

## User experience

1. User opens a Remotion workspace or creates one from a starter.
2. OMP detects the project and reads package metadata, Remotion entry point, compositions, dimensions, FPS, and duration.
3. User switches to Remotion Specialist and describes a scene or edit.
4. Agent inspects relevant files, proposes a short plan, edits only the selected workspace, and runs available static checks.
5. User opens Preview inside OMP. The embedded surface loads a trusted HTTPS preview page and exposes play/pause, seek, composition selection, and fit controls.
6. User chooses Render and selects composition, output format, quality, and optional frame range.
7. OMP submits a render job to the configured cloud provider and displays queued/running/succeeded/failed/cancelled states.
8. Completed output is shown with metadata and a download action.

## Scope

### MVP — first usable vertical slice

- Project detection for supported Remotion projects (Remotion dependency and a valid entry point).
- Read-only project inspection: compositions, dimensions, FPS, duration, entry point, package manager, and detected assets.
- A provider-agnostic Remotion specialist instruction profile.
- Workspace-scoped file edits with a visible change summary; no edits outside the selected project.
- Embedded preview shell in Compose using Android WebView.
- Preview must load only an explicitly configured HTTPS origin. No arbitrary URL navigation.
- Preview controls: composition, play/pause, seek, reload, fit-to-screen.
- Render request model and job-state UI, initially behind a cloud-renderer adapter.
- Clear unavailable/configuration states when preview hosting or cloud rendering is not configured.

### Post-MVP

- Timeline editor and visual layer manipulation.
- Asset library and upload manager.
- Render queue concurrency controls and retry policies.
- Multi-provider render backends.
- Collaborative editing and project templates.
- AI-assisted storyboard generation and shot breakdown.

## Android architecture

Keep the feature modular and separate from the existing terminal/PTY implementation.

Suggested packages (adapt to the repository's existing package layout):
- feature/remotion/project/ — project detection and metadata models.
- feature/remotion/agent/ — specialist profile and context builder.
- feature/remotion/preview/ — Compose screen, WebView host, preview state, bridge.
- feature/remotion/render/ — render request, provider interface, job state, repository.
- core/workspace/ — reuse existing workspace permission and path boundaries.

Core interfaces (conceptual; adapt to current Kotlin conventions):

    interface RemotionProjectDetector {
        suspend fun inspect(projectRoot: ProjectRoot): RemotionProject?
    }
    interface RemotionPreviewProvider {
        suspend fun createPreview(project: RemotionProject): PreviewSession
    }
    interface CloudRenderProvider {
        suspend fun submit(request: RenderRequest): RenderJob
        suspend fun getJob(jobId: String): RenderJob
        suspend fun cancel(jobId: String)
    }

Models must be serializable and independent from Compose:
- RemotionProject: root, entryPoint, packageManager, compositions, detectedRemotionVersion.
- RemotionComposition: id, width, height, fps, durationInFrames.
- PreviewSession: HTTPS URL, allowed origin, expiry, session id.
- RenderRequest: project reference, composition id, codec/container, quality, frame range.
- RenderJob: id, status, progress when available, output metadata, error summary.

## Embedded preview design

Use Android WebView hosted inside a Compose AndroidView. The preview page should run a web Remotion Player/Studio-compatible bundle and receive a narrowly scoped project/preview session.

Security and reliability:
- Require HTTPS and allowlist the exact preview host.
- Block navigation to untrusted origins and external intents by default.
- Do not expose filesystem APIs or secrets through addJavascriptInterface.
- If a JS bridge is needed, expose only typed playback/selection events and validate message origin and payload.
- Disable file access, universal file access, and mixed content.
- Handle WebView process death, renderer crash, network loss, loading, and expired preview sessions.
- Do not use a loopback/localhost auth or callback server.
- Never pretend the preview is live if the configured preview endpoint is unavailable.

The preview is a review surface, not a replacement for the full desktop Remotion Studio in MVP.

## Hybrid cloud rendering

Android performs authoring, metadata inspection, and job orchestration only. Final MP4 rendering runs on a Linux cloud worker with a pinned Node/Bun toolchain, Chromium dependencies, and a pinned Remotion version.

Provider contract:
- submit returns a durable job id quickly.
- getJob returns normalized status and progress.
- cancel is best-effort and idempotent.
- Output is stored in object storage and exposed via short-lived download URL.
- Job state survives app restarts.

Recommended first backend: a small authenticated render broker that dispatches a GitHub Actions workflow using a GitHub App installation token. Do not embed a personal access token or long-lived GitHub secret in the APK. Do not send repository secrets to project code. Use a dedicated render repository or tightly scoped allowlisted repositories.

Job states: queued, preparing, rendering, uploading, succeeded, failed, cancelled.
A job is succeeded only after the output upload is verified and a downloadable artifact reference exists.

## Specialist agent behavior

The Remotion profile must instruct the agent to:
- Inspect before editing; identify entry point and compositions first.
- Use Remotion APIs and versions actually present in the project.
- Prefer small, frame-deterministic components and reusable scenes.
- Use useCurrentFrame, interpolate, spring, Sequence, and AbsoluteFill appropriately.
- Respect composition FPS, dimensions, duration, and frame boundaries.
- Avoid browser-only APIs and non-deterministic values during rendering.
- Check asset paths, font loading, audio/video availability, and licensing.
- Run typecheck/lint/build or project-defined checks when available.
- Explain the changed files and unresolved render risks.
- Never claim a render succeeded without a verified cloud job result and output.

Suggested specialist workflow:
1. Inspect project.
2. Summarize composition plan.
3. Implement the smallest complete scene.
4. Run checks.
5. Open embedded preview.
6. Submit cloud render only after user action.

## Permissions and safety

- All file operations stay under the selected workspace root.
- Show changed file list and diff before destructive or broad refactors.
- No automatic deployment, publishing, or upload of source assets without user action.
- Render requests require explicit user action.
- Secrets are stored in Android Keystore only when they are user-owned credentials; backend credentials remain server-side.
- Apply file count, upload size, frame count, duration, and render timeout limits.
- Treat project files, prompts, assets, and remote preview content as untrusted input.

## Acceptance criteria

### Project and agent
- [ ] Detect a valid Remotion project and reject ordinary React projects with a clear explanation.
- [ ] Extract composition id, width, height, FPS, and duration.
- [ ] Agent can create/edit a simple title-card composition without changing files outside workspace.
- [ ] Agent reports checks accurately and never overstates validation.

### Embedded preview
- [ ] Preview opens inside OMP without launching an external browser.
- [ ] Play/pause and seek controls work.
- [ ] Only allowlisted HTTPS origin is loaded.
- [ ] Offline, expired session, WebView crash, and unsupported project states are visible and recoverable.
- [ ] Verified on a physical ARM64 Android device.

### Cloud render
- [ ] Submit creates a durable job id.
- [ ] Job state survives app restart.
- [ ] Failed jobs show actionable error summaries.
- [ ] Succeeded is shown only after output upload verification.
- [ ] MP4 download completes and file integrity is checked.
- [ ] No long-lived GitHub token is shipped in the APK.

## Delivery phases

1. Foundation: spec, project detector, metadata models, specialist profile.
2. Preview vertical slice: HTTPS preview contract, embedded WebView Compose screen, origin restrictions, playback bridge.
3. Cloud render vertical slice: broker contract, GitHub Actions worker, persistent job states, output storage.
4. Agent integration: project context, scoped edits, checks, preview handoff.
5. Device hardening: physical-device regression, lifecycle, network-loss, large-project and render-failure tests.

## Explicit non-goals

- Rendering full videos locally on Android.
- Running Chromium headless inside the Android app.
- Replacing OMP's terminal/runtime work.
- Requiring a separate Termux app for Remotion preview.
- Exposing arbitrary shell execution or arbitrary WebView navigation.
- Merging to main or deploying a backend as part of this specification change.
