# Voice input

Requirement: the chat's microphone button records an utterance and puts its transcription in the input field
([chat.md](../requirements/chat.md)).

## Design

`VoiceRecognitionService` in the client opens the system microphone through `javax.sound.sampled`, buffers PCM until the
user stops the recording, wraps it as WAV and sends it to Gemini through `transcribeAudio` in `llm`, with a short system
prompt asking for a verbatim transcription and `[?]` for anything unclear. The text is placed in the chat input for the
user to read and send, not sent directly, because a model can rewrite poor audio confidently.

Transcription is one-shot: nothing appears until the recording stops, and a "transcribing" indicator is shown meanwhile.

## Why Gemini

An offline recogniser (Vosk, small US-English model) was the first implementation and was unsatisfactory: poor on
conversational speech, accents and domain terms, no punctuation or casing, and a 40 MB model in the distribution.

Google Cloud Speech-to-Text was compared. It offers true streaming partials, per-minute pricing, word confidences and
**phrase hints**, which would let the KB's attribute names bias recognition, and it never hallucinates. Against that it
needs a GCP project, billing and credentials distinct from the AI Studio key that Gemini uses, plus another SDK.

Gemini reuses the client, key and billing already in place, punctuates naturally, and uses context to get domain terms
right; the cost is the absence of live partials. That was judged the right trade for a chat input the user reviews
before sending. If live dictation or hallucination on rule text becomes a problem, Speech-to-Text with phrase hints from
the open KB is the upgrade path, possibly only for the rule-building flow.

## Platform notes

Capture problems seen on macOS: the JVM inherits the microphone permission of whatever launched it (a terminal running
Gradle needs the permission); the packaged `.app` must be launched through LaunchServices for its usage description to
be honoured; a broken ad-hoc signature makes TCC deny silently; and Java Sound uses the system default input device.
