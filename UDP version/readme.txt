Note:
The primary objective of this project is to use the unreliable UDP instead of TCP transport protocol in the implementation of  both HTTP Client and HTTP file manager. To this end, the projects should manually ensure the reliability of the transport on top of UDP protocol by implementing Selective-Repeat ARQ technique. 

Selective Repeat is part of the automatic repeat-request (ARQ). With selective repeat, the sender sends a number of frames specified by a window size even without the need to wait for individual ACK from the receiver as in Go-Back-N ARQ. The receiver may selectively reject a single frame, which may be retransmitted alone; this contrasts with other forms of ARQ, which must send every frame from that point again. The receiver accepts out-of-order frames and buffers them. The sender individually retransmits frames that have timed out.[1]

It is important to emphasize that both the HTTP Client and the file manager should keep the same specifications as given in the previous assignments. The apps should be updated to be executed in the simulation environment as will be described in the following sections.

In addition to adjusting the previous apps (HTTP client and file manager server) to work with UDP and the provided simulation environment, it is required to implement the core functionalities of TCP protocol such as Mimicking the TCP three-way handshaking.

The demo file is include in this repositery.