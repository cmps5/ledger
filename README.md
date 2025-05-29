# Public Ledger for Auctions

Decentralized public blockchain designed to store and manage auction transactions in a secure and transparent manner. Built for the System and Data Security course (2024/2025), this project ensures data integrity, resilience, and decentralized trust using a custom blockchain layered on top of a Kademlia-based P2P network.

*Note: read the report for more info*.
### Build
    $ mvn clean install
### Run bootstrap node
    $  mvn exec:java -Dexec.mainClass=peer.BootStrap
### Run clients
    $ mvn exec:java -Dexec.mainClass=peer.Peer -Dexec.args="<ip> <port>"

There is a lot to improve, but maybe in the future.
