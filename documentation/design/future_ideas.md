# Future ideas
Improvements we might make in the future.

## Using AI to generate natural-language reports
One of RippleDown's great strengths is that it can be used to assemble a set of conclusions
for a case, with each conclusion being justified by a set of conditions that are comprehensible
to humans.

There are many applications where a set of facts is exactly what is required - for example
in laboratory workflow automation. In other applications, however, the output is for 
humans and a natural language report is required.

There are RippleDown implementations where rules are used to organise comments in such a way
that the output reads well. However, in these systems, a fair bit of the rule building effort
is focused on eliminating situations in which two conclusions are provided that have overlapping meanings.
For example a case might receive the conclusions
`Borderline high LDL and low HDL require monitoring.` and `Low HDL noted.`, which do not read well
together in a report. In a RippleDown system that produces such a report, a rule will
typically be added to remove the second conclusion with conditions along the lines of
those that justified the first.

A better approach to producing natural language reports from a RippleDown system might be
to use RippleDown to produce a set of very simple factual statements, and then to get
a generative AI system to produce a natural langauge report from these statements.
