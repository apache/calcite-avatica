# Agent guidance

Avatica is a JDBC/ODBC wire-protocol layer: a server that fronts a
local JDBC `DataSource` (typically Apache Calcite, but any JDBC driver
is supported), and a client-side JDBC driver that speaks the Avatica
wire protocol over HTTP or HTTPS.

## Security

See [SECURITY.md](./SECURITY.md) before reporting a vulnerability.
