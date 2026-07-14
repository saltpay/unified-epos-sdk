using System;
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using EposPosLinkExample.Helpers;
using Microsoft.UI.Dispatching;

namespace EposPosLinkExample.ViewModels;

public partial class TransactionHistoryViewModel : ObservableObject
{
    private readonly TransactionStore _store = TransactionStore.Instance;
    private readonly DispatcherQueue _dispatcherQueue = DispatcherQueue.GetForCurrentThread();

    public ObservableCollection<TransactionItem> Transactions { get; } = new();

    public bool HasNoTransactions => Transactions.Count == 0;

    public TransactionHistoryViewModel()
    {
        Transactions.CollectionChanged += (_, _) => OnPropertyChanged(nameof(HasNoTransactions));
        _store.Changed += OnStoreChanged;
        Reload();
    }

    // Store updates may arrive on the JSON-RPC reader thread, so hop onto the UI thread
    // before touching the bound collection.
    private void OnStoreChanged(object? sender, EventArgs e) => _dispatcherQueue.TryEnqueue(Reload);

    private void Reload()
    {
        Transactions.Clear();
        foreach (var record in _store.Snapshot)
        {
            Transactions.Add(new TransactionItem(record));
        }
    }
}
