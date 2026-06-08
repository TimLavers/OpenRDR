# Using Optical Character Recognition in Tests

Our end-to-end tests read the state of the UI using the Accessability API.
This is generally a robust and simple way to get the text showing, find components, and so on.
However, there are some situations in which this approach does not work.

To handle these situations, we use Optical Character Recognition (OCR) to read the UI state.
This works as follows:
- from the Accessability API we can get the location and bounds of a component
- `java.awt.Robot` can be used to take a screenshot
- An OCR library can be used to extract the text in the screenshot

_Note that for operating the user interface (typing text, clicking buttons, etc) we use the Accessability API or 
the `Robot`._

## Options for OCR
The main decision is whether to use an on-device library or a cloud-based service for OCR.
A cloud-based service has the following advantages:
- No need to install an OCR library and configure it in our test code
- Likely to do a better job than an on-device library, especially with non-English input

On the other hand, an on-device service:
- Will be much faster and have lower latency
- May be cheaper than using a cloud-based service

### Tesseract
We have run some experiments using the freely available and industry-standard Tesseract library.
The results so far have been good. However, if we are to continue with this approach, we will need to
add configuration code to our test platform.

### Gemini
We've also used Gemini 2.5 for OCR. This has not been noticeably slower than Tesseract.
The only reasons not just to use Gemini are:
- the possible cost
- if we are polling a UI component to wait for it to be in a certain state, Gemini's latency might really become a problem.


