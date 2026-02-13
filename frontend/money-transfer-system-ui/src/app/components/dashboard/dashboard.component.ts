import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { AccountService } from '../../services/account.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  holderName = '';
  balance = 0;
  accountNumber = '';
  isLoading = true;

  constructor(private accountService: AccountService, private router: Router) {}

  ngOnInit() {
    const accountId = localStorage.getItem('accountId');
    if (accountId) {
      this.accountService.getAccount(accountId).subscribe({
        next: acc => {
          this.holderName = acc.holderName;
          this.balance = acc.balance;
          this.accountNumber = acc.accountNumber ?? accountId;
          this.isLoading = false;
        },
        error: () => {
          this.holderName = '';
          this.balance = 0;
          this.isLoading = false;
        }
      });
    } else {
      this.holderName = '';
      this.balance = 0;
      this.isLoading = false;
    }
  }

  navigateToTransfer() {
    this.router.navigate(['/transfer']);
  }

  navigateToHistory() {
    this.router.navigate(['/history']);
  }

  logout() {
  localStorage.removeItem('authToken');
  localStorage.removeItem('accountId');
  this.router.navigate(['/login']);
}

}
