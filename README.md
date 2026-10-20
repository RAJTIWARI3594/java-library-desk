# Java Library Desk

A persistent lending desk with book registration, checkout, return, availability and loan history. Atomic file replacement and a process lock protect the local ledger.

## Quick start

```sh
sh build.sh
java -cp build LibraryDesk demo.tsv add B1 "Synthetic Java Handbook"
java -cp build LibraryDesk demo.tsv borrow B1 SYN-READER-1
java -cp build LibraryDesk demo.tsv list
java -cp build LibraryDesk demo.tsv return B1
java -cp build LibraryDesk demo.tsv history
```
Install a JDK 17+; no Maven, server or external dependencies required.

## Tests

`sh test.sh`

## Scope and limitations

Local CLI with no multiuser authentication, due-date reminders or fines. Unencrypted ledger; use synthetic borrower IDs, not personal details. Locks require a local filesystem supporting Java file locks; do not put live state on network drives. Back up ledgers yourself. Never edit the TSV by hand while running.

## Attribution and sample data

Built for Raj's portfolio with Wabi AI assistance. Sample data is invented and labelled synthetic; no real customer, employer or private account records are included. This is a learning/portfolio implementation, not a claim of commercial work or production deployment.

See source and tests for implementation decisions. Use invented data when exploring.

## How it works

CLI acquires ledger lock → reads versioned Base64-encoded TSV → validates operation → atomically replaces ledger. History is retained as append-only events through the interface.

## Verified example

Actual JDK17 compiled source; separate CLI processes added, borrowed, listed, returned and listed one synthetic book with three history events.

See [VALIDATION.md](VALIDATION.md) for tested environments and scope.
