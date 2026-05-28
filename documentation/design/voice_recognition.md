# Voice recognition

The chat panel supports voice input so the user can dictate rule conditions, comments, and
free-form chat instead of typing. This document records the history of the implementation
and the design for the next iteration.

## Initial implementation: Vosk (offline)

The first implementation used [Vosk](https://alphacephei.com/vosk/) via its JNI bindings.
The small US English model (`vosk-model-small-en-us-0.15`, ~40 MB, WER ~10% on clean read
speech) was bundled into the UI resources. `VoiceRecognitionService` opened the system
microphone via `javax.sound.sampled`, fed PCM frames into a `Recognizer`, and exposed
streaming partials plus final results to the chat panel.

In practice this was **very unsatisfactory**:

- Accuracy on conversational speech, accents, and domain terms (attribute names, drug
  names, units) was poor.
- Punctuation and casing were absent.
- The bundled model added significant size to the distribution.
- Larger Vosk models would help but still trail modern alternatives by a wide margin.

## Options considered

Two Google services were compared for the next iteration. Both are markedly better than
Vosk. The same API key cannot be used for both — the existing Gemini key is issued by Google
AI Studio and only authenticates `generativelanguage.googleapis.com`, whereas Cloud
Speech-to-Text requires a GCP project, billing account, and its own credentials.

### Option 1: Gemini audio input

Send recorded audio to the existing Gemini model as another input modality.

**Pros**

- Reuses the existing Gemini API key, client, and billing — no new infrastructure.
- Context-aware transcription: the model uses the conversation context to disambiguate
  domain terms.
- Clean, naturally punctuated and capitalised output; disfluencies tidied.
- Instruction-following: the same call can be told to transcribe verbatim, normalise
  numeric values, etc.
- Quality is close to Cloud Speech-to-Text on raw accuracy and often subjectively better
  on readability.

**Cons**

- Being an LLM, it can hallucinate or "helpfully" rewrite on poor audio rather than
  emit low-confidence text.
- No real streaming transcription; audio is sent as a recorded utterance and a result
  comes back after 1–3 s.
- No word-level timestamps and no real confidence scores.
- Per-token billing rather than per-minute.

### Option 2: Google Cloud Speech-to-Text

Purpose-built ASR (`chirp_2` is the current flagship model), with optional medical and
phone-call variants.

**Pros**

- True streaming with partials — closest fit to the current Vosk UX.
- **Speech adaptation / phrase hints**: a vocabulary (e.g. derived from `KB.attributes`)
  can be supplied with boost weights, giving a large quality lift on domain terms.
- Word-level timestamps, confidence scores, diarisation, automatic punctuation.
- Predictable per-minute pricing; will not hallucinate — it transcribes what was said.

**Cons**

- Separate GCP project, billing account, and credentials (service account or restricted
  API key) — the Gemini key does not work.
- New SDK dependency (`com.google.cloud:google-cloud-speech`).
- Punctuation and casing are good but less natural than an LLM's.
- More setup overhead per deployment.

## Proposed design: Gemini audio

The next iteration will use **Gemini audio input** behind the existing
`SpeechRecognizer` interface

### Flow

1. The user clicks the microphone button in the chat panel.
2. `VoiceRecognitionService` opens the microphone via `javax.sound.sampled` (unchanged
   from today) and writes PCM frames into an in-memory buffer.
3. A simple voice-activity detector (silence threshold + timeout, or an explicit second
   click by the user) decides when the utterance is complete.
4. The buffered PCM is wrapped as a WAV blob and sent to Gemini as a `Part` of a single
   `generateContent` call alongside a short system prompt instructing the model to
   transcribe verbatim and not to invent missing words.
5. The returned text is delivered to the chat panel as the final result, the same
   callback the Vosk path used

### Implementation notes

- Add a new `GeminiRecognizer` (or `GeminiSpeechBackend`) implementing the existing
  `SpeechRecognizer` abstraction so the chat panel is unaware of the backend.
- Reuse the existing `Client` from `Gemini.kt`; no new SDK or auth surface.
- Keep `VoskRecognizer` selectable via configuration for offline operation.
- Partial results: while the request is in flight the panel can show an indeterminate
  "transcribing…" indicator. True streaming partials are out of scope for this iteration.
- A short system prompt should bias the model toward verbatim transcription and away
  from helpful rewriting, e.g. *"Transcribe the user's speech literally. Do not add,
  remove, or rephrase content. If a word is unclear, write `[?]`."*

### Future work

- **Streaming partials**: migrate to Google Cloud Speech-to-Text (`chirp_2`) with
  phrase hints derived from the active KB's attribute names. This gives a Vosk-like
  live-partial UX and the best raw accuracy on domain terms, at the cost of standing
  up a GCP project and adding a new SDK.
- **Hybrid**: use Gemini for free-form chat dictation and Cloud STT (with phrase
  hints) specifically for the rule-building dictation flow, where hallucination risk
  matters most.

### Problems encountered

If no text appears, the following may be the reasons:

- No live partial transcript — by design (Gemini is single-shot, transcribes only after stop). You need to turn off the
  mic before the text is processed.
- ./gradlew cuST ran the JVM as a child of Terminal — Terminal needed mic permission. Fixed by granting Terminal mic +
  restarting Gradle daemon.
- OpenRDR.app was launched via its inner Mach-O binary instead of LaunchServices, so its NSMicrophoneUsageDescription
  was ignored. Fixed in start-demo.sh:97-115 to use open -W for the .app.
- Bundle's ad-hoc signature was broken (spctl reported "a sealed resource is missing or invalid"), so on macOS Tahoe TCC
  silently denied with no prompt. Fixed by codesign --force --deep --sign - after assembly.
- The grant finally appeared, but capture was still silent because Java Sound uses the macOS system default input
  device, which was the (non-working) built-in MacBook mic, not the Pro Stream webcam. Fixed by switching the system
  default input.