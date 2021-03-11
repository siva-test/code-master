package com.db.awmd.challenge.web;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferAccountRequest;
import com.db.awmd.challenge.exception.TransactionExceptionMessage;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransferAmountService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/transferAmounts")
@Slf4j
public class TransferAmountsController {

	private final TransferAmountService transferAmountService;
	private final AccountsService accountsService;

	@Autowired
	public TransferAmountsController(AccountsService accountsService,TransferAmountService transferAmountService) {
		this.transferAmountService = transferAmountService;
		this.accountsService = accountsService;
	}


	@PostMapping(path ="/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
	public Object transferAmount(@RequestBody @Valid TransferAccountRequest transferAccountRequest){

		try {
			return this.transferAmountService.transferAmount(transferAccountRequest);
		} catch (TransactionExceptionMessage tem) {
			return new ResponseEntity<>(tem.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(path = "/{accountId}")
	public ResponseEntity<Object> getAccount(@PathVariable String accountId) {
		log.info("Retrieving account for id {}", accountId);
		Account account=accountsService.getAccount(accountId);
		if(account ==null) {
			return new ResponseEntity<>(accountId+" account not avaliable ",HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(account,HttpStatus.OK);
	}

}
