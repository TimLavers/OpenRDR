# OpenRDR
OpenRDR is an implementation of the Ripple-Down Rule knowledge acquisition protocol (RDR).

In RDR, human experts look at sets of related data, called cases, make comments
on them, and justify the comments, within a system that collects these
comments and justifications into an executable form that can be used to
make comments on other cases.

Typically, a case might be a set of pathology results for a patient, and the
expert a clinical pathologist. The comments are diagnoses like 
"Normal T4 and TSH are consistent with a euthyroid state." and the justifications
are logical predicates such as "T4 in normal range".

When experts write these comments and justifications, the RDR system checks to
see if any previously interpreted cases would have their interpretations changed
by the new information. Any such conflicts are presented to the expert. By comparing
the conflict case with the case for which the new information is being added,
the expert is motivated to add extra justifications that differentiate the
two cases and prevent over-generalisations.