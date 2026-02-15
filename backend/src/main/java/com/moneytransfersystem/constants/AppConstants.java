package com.moneytransfersystem.constants;

public class AppConstants {

    private AppConstants(){

    }
        public static final String ACCOUNT_NOT_FOUND =
                "Account not found";

        public static final String ACCOUNT_NOT_ACTIVE =
                "Account is not active";

        public static final String INSUFFICIENT_BALANCE =
                "Insufficient balance";

        public static final String INVALID_DEBIT_AMOUNT =
                "Withdrawal amount must be positive";

        public static final String INVALID_CREDIT_AMOUNT =
                "Deposit amount must be positive";


        public static final String DUPLICATE_TRANSACTION =
                "Transaction with this key already exists";

        public static final String SAME_ACCOUNT_TRANSFER =
                "Source and destination accounts cannot be the same";

        public static final String SENDER_ACCOUNT_NOT_FOUND =
                "Sender account not found";

        public static final String RECEIVER_ACCOUNT_NOT_FOUND =
                "Receiver account not found";
    }

