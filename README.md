# Public Ledger for Auctions

### Build
    $ mvn clean install
### Run bootstrap node
    $  mvn exec:java -Dexec.mainClass=peer.BootStrap
### Run clients
    $ mvn exec:java -Dexec.mainClass=peer.Peer -Dexec.args="<ip> <port>"

