import { Component, OnInit } from '@angular/core';
import { AccountService } from '../../services/account';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class Dashboard implements OnInit {
  holderName = '';
  balance = 0;

  constructor(private accountService: AccountService) {}

  ngOnInit() {
    this.accountService.getAccount().subscribe(acc => {
      this.holderName = acc.holderName;
      this.balance = acc.balance;
    });
  }
}
