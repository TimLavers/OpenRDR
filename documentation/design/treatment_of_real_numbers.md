# Treatment of real numbers
It is best to use arbitrary-precision number formats such as Java's `BigDecimal`
in software that does precise calculations. This is best practice in financial systems.

Ideally, we would use an arbitrary precision system.
However, we would like our rule conditions and other calculations to run in the client
side (browser) as well as on the server, and so are limited to using libraries 
that are available on Kotlin Multiplatform. There are no well-supported arbitrary
precision libraries available on Kotlin Multiplatform so we must stick with
using `Double` to represent real numbers.

This however might not be a big problem because our main use-case is chemical
pathology, which deals in fairly imprecise measurements anyway.

Our main difficulty is developing an approach that does not allow the subtleties
of floating-point numbers to confuse the users.

Here's an example how confusion may arise. Suppose that a user is looking at a case
with a test result of `0.95` with reference range `[1.0, 4.0]` and is adding a 
condition along the lines of `value is within 5% of lower reference range`.
The value in the case is right on the calculated limit of 95%. Whether the 
condition evaluates true or false may depend on tiny calculation errors
inherent in floating point arithmetic and totally unknown to pathologists.

To deal with this problem, the implementation of this syntax returns true
when the numbers being compared are extremely close. This is probably less
likely to cause confusion amongst users than returning false in such situations,
because the users are unlikely to conceive of conditions that reveal that
a value of `0.9499999` is being treated as equal to `0.95`.
