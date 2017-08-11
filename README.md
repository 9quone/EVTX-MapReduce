# EVTX-MapReduce
An Event Log Parser in Java compatible with Hadoop MapReduce to ingest the binary data of terabytes of event log files and extract the time created and event ID of all the events stored in the logs. 

The Hadoop framework utilizes a Distributed File System across a multitude of DataNodes to parallel process Big Data, enabling high performance computing on commodity servers. This MapReduce algorithm processes hundreds of thousands of events by generating key-value pairs of the Event ID and corresponding Time (Mapper phase) and aggregating them to produce a list sorted by event frequency (Reducer phase). 
