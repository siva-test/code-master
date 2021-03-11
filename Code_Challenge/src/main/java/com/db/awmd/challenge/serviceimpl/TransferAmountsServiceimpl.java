package com.db.awmd.challenge.serviceimpl;

import static java.lang.String.format;
import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.constants.ApplicaitonConstants;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferAccountRequest;
import com.db.awmd.challenge.exception.TransactionExceptionMessage;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.EmailNotificationService;
import com.db.awmd.challenge.service.TransferAmountService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TransferAmountsServiceimpl implements TransferAmountService{

	@Autowired
	private AccountsRepository accountsRepository;


	private final Lock acctLock;
	boolean accuntsValidationFlag=false;

	public TransferAmountsServiceimpl()
	{
		acctLock = new ReentrantLock();
	}

	public Object transferAmount(TransferAccountRequest transferAccountRequest) {
		log.info("Retrieving transaction information from TransferAccountRequest  object");
		String fromAccountNumber = transferAccountRequest.getFromAccountNumber();
		String toAccountNumber = transferAccountRequest.getToAccountNumber();
		BigDecimal amount = transferAccountRequest.getAmount();


		log.info("transferAmount {} : ");
		log.info("accountNumberFrom : "+fromAccountNumber);
		log.info("accountNumberTo : "+toAccountNumber);
		log.info("amountToTransfer : "+amount);

		if(accountValidations(fromAccountNumber, toAccountNumber, amount)) {
			Account accountFrom = accountsRepository.getAccount(fromAccountNumber);
			Account accountTo = accountsRepository.getAccount(toAccountNumber);
			try
			{
				if(withdrawal(accountFrom,accountTo,amount)){
					if(deposit(accountFrom,accountTo,amount)) {
						log.info(ApplicaitonConstants.TRANSACTION_SUCESSFULL);
					}
					else
						throw new TransactionExceptionMessage(format(ApplicaitonConstants.TRANSACTION_FAILED));
				}
			} catch (Exception e) {throw new TransactionExceptionMessage(format(ApplicaitonConstants.TRANSACTION_FAILED));}
		}
		else
			return ApplicaitonConstants.TRANSACTION_FAILED;
		return ApplicaitonConstants.TRANSACTION_SUCESSFULL;

	}

	public boolean accountValidations(String accountNumberFrom, String  accountNumberTo, BigDecimal amountToTransfer)
	{
		if(amountToTransfer.doubleValue()>=1) {
			getAccount(accountNumberFrom, ApplicaitonConstants.SOURCE_ACCOUNT_NOT_EXIST);
			if(checkBalance(accountNumberFrom).compareTo(amountToTransfer)  <= - 1){
				log.info(format(ApplicaitonConstants.NOT_ENOUGH_BALANCE, accountNumberFrom));
				throw new TransactionExceptionMessage(format(ApplicaitonConstants.NOT_ENOUGH_BALANCE, accountNumberFrom));}
			getAccount(accountNumberTo, ApplicaitonConstants.DESTINATION_ACCOUNT_NOT_EXIST);
			if(accountNumberFrom.compareTo(accountNumberTo)==0)
			{
				log.info(format(format(ApplicaitonConstants.ACCOUNT_MATCH)));
				throw new TransactionExceptionMessage(format(ApplicaitonConstants.ACCOUNT_MATCH));
			}
			return accuntsValidationFlag = true;
		}
		else {
			throw new TransactionExceptionMessage(format(ApplicaitonConstants.TRANS_AMOUNT_VALUE,amountToTransfer));
		}
	}

	private Account getAccount(String accountNumber, String errorReason) {
		Account ret = accountsRepository.getAccount((accountNumber));
		if (ret == null) {
			log.info(format(errorReason, accountNumber));
			throw new TransactionExceptionMessage(format(errorReason, accountNumber));
		}
		return ret;
	}

	public BigDecimal checkBalance(String accountNumberFrom){
		try {
			acctLock.lock();
			Account accountFrom = accountsRepository.getAccount(accountNumberFrom);
			BigDecimal balance = accountFrom.getBalance();
			return balance;
		}
		finally {
			acctLock.unlock();
		}
	}

	boolean withdrawal(Account accountFrom,Account accountTo,BigDecimal amountToTransfer)
	{ 
		try{   
			acctLock.lock();
			if(accountFrom.getBalance().compareTo(amountToTransfer)  >=0){
				accountFrom.setBalance(accountFrom.getBalance().subtract(amountToTransfer));
				log.info("withdrawal {} : Frome account balance after withdrawal " +accountFrom.getBalance());
				new EmailNotificationService().notifyAboutTransfer(accountFrom,
						"amount of "+amountToTransfer+" got debited from " +accountFrom.getAccountId()+
						" and credited to "+ accountTo.getAccountId());

				log.info("withdrawal {} : withdrawal Completed");
				return true;
			}
			else {
				log.info("withdrawal {} : withdrawal Failed");
				return false;}}
		finally{
			acctLock.unlock(); 
		}

	}
	boolean deposit(Account accountFrom,Account accountTo,BigDecimal amountToTransfer)
	{
		try{   
			acctLock.lock();
			accountTo.setBalance(accountTo.getBalance().add(amountToTransfer));
			log.info("deposit {} : To account balance after deposit " +accountTo.getBalance());
			new EmailNotificationService().notifyAboutTransfer(accountTo,
					"amount of "+amountToTransfer+" got credited to "+accountTo.getAccountId()
					+" from account " +accountFrom.getAccountId());
			log.info("deposit {} : deposit Completed");
			return true;
		}
		finally{
			acctLock.unlock(); 
		}  

	}
}
