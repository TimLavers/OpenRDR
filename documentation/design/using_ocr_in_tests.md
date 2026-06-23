# Using Optical Character Recognition in Tests

Our end-to-end tests read the state of the UI using the Accessibility API.
This is generally a robust and simple way to get the text showing, find components, and so on.
However, there are some situations in which this approach does not work.

To handle these situations, we use Optical Character Recognition (OCR) to read the UI state.
This works as follows:
- from the Accessability API we can get the location and bounds of a component
- `java.awt.Robot` can be used to take a screenshot
- An OCR library can be used to extract the text in the screenshot

_Note that for operating the user interface (typing text, clicking buttons, etc) we use the Accessibility API or 
the `Robot`._

It is true that having two different approaches to reading the state of the user interface is a bit inconsistent
and carries some overhead. But there are circumstances in which there seems to be no way of getting the information
a test needs from the Accessibility API.

Also, OCR testing is more "genuine" in the sense that it is checking what a human really sees.
If the text is tiny or does not contrast with other visual components then an OCR tool will fail to read it
clearly, just as a human would. So it might be nice to keep the option of this kind of testing as a
part of our toolset.

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

## Decision: use Gemini
For now, we will stick to using Gemini alone for OCR testing. This avoids users needing to install and configure Tesseract
and avoids the need to configure it properly in this project. Gemini has not proven to be too slow or too expensive
and is also likely to become cheaper, faster and more powerful over the next few years. We can easily change
back to Tesseract or some other local tool if necessary.


