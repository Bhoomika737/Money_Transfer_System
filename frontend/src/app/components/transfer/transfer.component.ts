import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AccountService } from '../../services/account.service';
import { TransferService } from '../../services/transfer.service';
import { MatSnackBar,MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../services/auth.service';
@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './transfer.component.html',
  styleUrls: ['./transfer.component.css']
})
export class TransferComponent implements OnInit {
  txnForm!: FormGroup;
  userName = '';
  funds = 0;
  accountNumber = '';
  fetchingFunds = true;
  processing = false;
  operationDone = false;
  operationData: any;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private transferService: TransferService,
    private router: Router,
    private snackBar:MatSnackBar,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.txnForm = this.fb.group({
      targetId: ['', [Validators.required, Validators.pattern(/^[A-Za-z]+\d+$/)]],
      value: ['', [Validators.required, Validators.min(1)]],
      remarks: ['']
    });

    const accountId = this.authService.getAccountId() ?? '';
    const effectiveAccountId = accountId;
    if (effectiveAccountId) {
      this.accountService.getAccount(effectiveAccountId).subscribe({
        next: acc => {
          this.userName = acc.holderName;
          this.funds = acc.balance;
          this.accountNumber = acc.accountNumber ?? effectiveAccountId;
          this.fetchingFunds = false;
        },
        error: () => {
          this.userName = 'Unknown';
          this.funds = 0;
          this.accountNumber = effectiveAccountId;
          this.fetchingFunds = false;
        }
      });
    } else {
      this.userName = 'Unknown';
      this.funds = 0;
      this.accountNumber = 'N/A';
      this.fetchingFunds = false;
    }
  }

  get form() {
    return this.txnForm.controls;
  }

  maskAccountNumber(): string {
    if (!this.accountNumber) return 'N/A';
    return this.accountNumber.replace(/.(?=.{4})/g, '*');
  }

  resetForm() {
    this.txnForm.reset();
    this.processing = false;
    this.operationDone = false;
    this.operationData = null;
  }

  doTransaction() {
  if (this.txnForm.invalid) return;

  const amount = Number(this.txnForm.value.value);

  this.processing = true;
  const payload = {
    fromAccountId: this.accountNumber,                 // logged-in account
    toAccountId: this.txnForm.value.targetId,          // form input
    amount: amount,                                    // numeric
    remarks: this.txnForm.value.remarks,               // optional
    idempotencyKey: crypto.randomUUID()                // unique key
  };

  this.transferService.transferFunds(payload).subscribe({
    next: res => {
      // Handle both success and failed responses from backend
      const transaction = res.transaction || res;
      this.operationData = transaction;
      this.operationDone = true;
      this.processing = false;

      // Notify user
      if (transaction.status === 'SUCCESS') {
        this.notify('Transfer completed successfully!', 2000);
      } else if (transaction.status === 'FAILED') {
        this.notify('Transfer failed: ' + (transaction.failureReason || 'Unknown reason'), 4000);
      }
      
      // Refresh account balance (for successful transfers)
      const accountId = this.authService.getAccountId();
      if (accountId && transaction.status === 'SUCCESS') {
        this.accountService.getAccount(accountId).subscribe({
          next: acc => {
            this.userName = acc.holderName;
            this.funds = acc.balance;
            this.accountNumber = acc.accountNumber ?? accountId;
          }
        });
      }
    },
    error: (err) => {
      this.processing = false;
      
      // Check if error response contains a saved transaction (400 response)
      if (err.error?.transaction) {
        const transaction = err.error.transaction;
        this.operationData = transaction;
        this.operationDone = true;
        this.notify('Transfer failed: ' + (err.error.reason || err.error.message), 4000);
      } else {
        // Generic error without transaction -> show inline failure card with available info
        const backendMsg = err.error?.message || err.error?.error || 'Transaction failed. Please try again.';
        this.operationData = {
          status: 'FAILED',
          failureReason: backendMsg,
          id: '—',
          amount: Number(this.txnForm.value.value) || 0,
          fromAccountId: this.accountNumber,
          toAccountId: this.txnForm.value.targetId,
          remarks: this.txnForm.value.remarks
        };
        this.operationDone = true;
        this.notify(backendMsg, 3000);
      }
    }
  });
}
notify(msg: string, dur: number): void {
    this.snackBar.open(msg, 'X', {
      duration: dur,
      panelClass: ['snackbar']
    });
  }
  goBack() {
    this.router.navigate(['/dashboard']);
  }
}
