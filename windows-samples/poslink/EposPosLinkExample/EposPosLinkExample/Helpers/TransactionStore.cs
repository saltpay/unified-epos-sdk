using System;
using System.Collections.Generic;
using System.Linq;
using EposPosLinkExample.Models;

namespace EposPosLinkExample.Helpers;

// In-memory, non-persisted transaction history for this app session. Records arrive from SDK
// callbacks on any thread, so every access is guarded by _gate.
internal sealed class TransactionStore
{
    public static TransactionStore Instance { get; } = new();

    private readonly object _gate = new();
    private readonly List<TransactionRecord> _transactions = new();

    // Observers should re-read Snapshot; the History view model marshals this onto the UI thread.
    public event EventHandler? Changed;

    // A newest-first copy, safe to read from any thread.
    public IReadOnlyList<TransactionRecord> Snapshot
    {
        get
        {
            lock (_gate)
            {
                return _transactions.ToList();
            }
        }
    }

    // Adds the record, or replaces an existing one with the same id, keeping newest first.
    public void Upsert(TransactionRecord record)
    {
        lock (_gate)
        {
            _transactions.RemoveAll(t => t.Id == record.Id);
            _transactions.Insert(0, record);
        }

        Changed?.Invoke(this, EventArgs.Empty);
    }

    // Stops the matching payment's row from offering Refund once its refund succeeds.
    // Only payments carry a non-null gateway payment id, so matching on it is enough.
    public void MarkRefunded(string gatewayPaymentId)
    {
        lock (_gate)
        {
            var i = _transactions.FindIndex(t => t.GatewayPaymentId == gatewayPaymentId);
            if (i >= 0)
            {
                _transactions[i] = _transactions[i] with { IsRefunded = true };
            }
        }

        Changed?.Invoke(this, EventArgs.Empty);
    }
}
