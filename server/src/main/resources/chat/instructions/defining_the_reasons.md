# Instructions for defining the reasons for an action

Ask the they want to provide a reason for the action.

If yes, collect the natural language reason and transform it using the "{{TRANSFORM_REASON}}" function. The function
will return a JSON object with "isTransformed" and "message". The value of "message" should be included in your response
to the user. See the <Transform reason> instructions below.

Keep asking for further reasons until the user indicates they are done or does not want to provide any more reasons.
