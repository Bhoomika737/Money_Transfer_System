import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { HistoryService } from '../../services/history.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.scss']
})
export class HistoryComponent implements OnInit {
  accountId: string = '';
  records: any[] = [];
  cols: string[] = ['date', 'type', 'account', 'amount', 'status', 'remarks'];
  busy = true;
  empty = false;

  constructor(
    private transactionService: HistoryService,
    private snackBar: MatSnackBar,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.accountId = this.authService.getAccountId() ?? '';
    this.loadHistory();
  }

  loadHistory(): void {
  this.busy = true;
  this.transactionService.fetchTransactionHistory(this.accountId).subscribe({
    next: (res: any[]) => {
      this.records = res.sort((a, b) => {
        return new Date(b.createdOn).getTime() - new Date(a.createdOn).getTime();
      });
      this.busy = false;
      this.empty = res.length === 0;
      this.notify('History loaded successfully', 2000);
    },
    error: (err) => {
      this.busy = false;
      const backendMsg = err.error?.message || 'Failed to load records';
      this.notify(backendMsg, 3000);
    }
  });
}


  getType(txn: any): string {
    return txn.fromAccountId === this.accountId ? 'DEBIT' : 'CREDIT';
  }

  getOther(txn: any): string {
    return txn.fromAccountId === this.accountId ? txn.toAccountId : txn.fromAccountId;
  }

  formatTs(ts: string): string {
    const d = new Date(ts);
    return d.toLocaleDateString('en-IN', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  reload(): void {
    this.loadHistory();
    this.notify('Refreshed', 2000);
  }

  notify(msg: string, dur: number): void {
    this.snackBar.open(msg, 'X', {
      duration: dur,
      panelClass: ['snackbar']
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
 
}

