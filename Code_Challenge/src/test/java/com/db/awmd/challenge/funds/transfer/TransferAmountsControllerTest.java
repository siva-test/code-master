package com.db.awmd.challenge.funds.transfer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferAccountRequest;
import com.db.awmd.challenge.exception.TransactionExceptionMessage;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransferAmountService;
import com.db.awmd.challenge.web.TransferAmountsController;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransferAmountsControllerTest {
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;
	@Mock
	@Autowired
	AccountsService accountsService;
	@InjectMocks
	@Autowired
	TransferAmountsController controller;


	@Mock
	@Autowired
	private TransferAmountService service;


	@Before
	public void prepareMockMvc() {
		this.mockMvc = webAppContextSetup(this.webApplicationContext).build();
	}

	@Test
	public void trasnferFundsWithOutFromAndToAccounts() throws Exception {
		TransferAccountRequest transferAccountRequest =new TransferAccountRequest("", "",  new BigDecimal("1000"));

		Mockito.when(service.transferAmount(transferAccountRequest)).thenReturn(null);
		this.mockMvc.perform(post("/v1/transferAmounts/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	public void tramsferFundsWithoutBalance() throws Exception {
		TransferAccountRequest transferAccountRequest =new TransferAccountRequest("Id-127", "Id-128",  new BigDecimal("0"));
		Mockito.when(service.transferAmount(transferAccountRequest)).thenReturn(null);
		this.mockMvc.perform(post("/v1/transferAmounts/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\":\"Id-127\",\"toAccountNumber\":\"Id-128\"}")).andExpect(status().isBadRequest());
	}

	@Test
	public void transferFundsWithoutBalanceAndWithoutAccounts() throws Exception {
		TransferAccountRequest transferAccountRequest =new TransferAccountRequest("0", "0",  new BigDecimal("0"));
		Mockito.when(service.transferAmount(transferAccountRequest)).thenReturn(null);
		this.mockMvc.perform(post("/v1/transferAmounts/transfer").contentType(MediaType.APPLICATION_JSON))
		.andExpect(status().isBadRequest());
	}

	@Test
	public void transferAmountWithNegativeBalance() throws Exception {
		TransferAccountRequest transferAccountRequest =new TransferAccountRequest("Id-123", "Id-128",  new BigDecimal("-1000"));
		Mockito.when(service.transferAmount(transferAccountRequest)).thenReturn(null);
		this.mockMvc.perform(post("/v1/transferAmounts/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\":\"Id-123\",\"toAccountNumber\":\"Id-128\",\"amount\":-1000}")).andExpect(status().isBadRequest());
	}


	@Test
	public void duplicateAccountsTransfer() throws Exception 
	{ 
		Account account = new Account("Id-131"); account.setBalance(new BigDecimal(1000)); 
		Account account1 = new Account("Id-131"); account1.setBalance(new BigDecimal(1000));
		Mockito.doNothing().when(accountsService).createAccount(account);
		Mockito.doNothing().when(accountsService).createAccount(account1);
		TransferAccountRequest transferAccountRequest = new TransferAccountRequest(account.getAccountId(),account1.getAccountId(),new  BigDecimal(500));
		Mockito.when(service.transferAmount(transferAccountRequest)).thenReturn(null);
		this.mockMvc.perform(post("/v1/transferAmounts/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\":\"Id-131\",\"toAccountNumber\":\"Id-131\",\"balance\":500}")).andExpect(status().isBadRequest());
	}

	@Test
	public void fundsTransfer() throws Exception 
	{ 
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"1234\",\"balance\":1000}")).andExpect(status().isCreated());
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"1235\",\"balance\":1000}")).andExpect(status().isCreated());
		//TransferAccountRequest transferAccountRequest = new TransferAccountRequest("1234","1235",new BigDecimal(500));
		try {
			//Mockito.when(service.transferAmount(transferAccountRequest)).thenReturn(transferAccountRequest);

			mockMvc.perform(post("/v1/transferAmounts/transfer").contentType(MediaType.APPLICATION_JSON)
					.content("{\"fromAccountNumber\":\"1234\",\"toAccountNumber\":\"1235\",\"amount\":500}")).andExpect(status().isOk());
		} catch (TransactionExceptionMessage ex) {
			assertThat(ex.getMessage()).isEqualTo("Transaction was not Sucessfull");
		}
	}

	@Test
	public void transationAmountGraterthanBalanceAmountInFromAccount()throws Exception {

		Account account = new Account("Id-154"); account.setBalance(new BigDecimal(1000)); 
		Account account1 = new Account("Id-156"); account1.setBalance(new BigDecimal(1000));
		Mockito.doNothing().when(accountsService).createAccount(account);
		Mockito.doNothing().when(accountsService).createAccount(account1);

		TransferAccountRequest transferAccountRequest = new TransferAccountRequest("Id-154","Id-156",new  BigDecimal(500));
		Mockito.when(service.transferAmount(transferAccountRequest)).thenReturn(transferAccountRequest);
		this.mockMvc.perform(post("/v1/transferAmounts/transfer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccountNumber\":\"Id-154\",\"toAccountNumber\":\"Id-156\",\"amount\":600000}")).andExpect(status().isBadRequest());
	}


	@Test
	public void fromAccountAndToAccountsAreWrong()throws Exception {

		TransferAccountRequest transferAccountRequest = new TransferAccountRequest("Id-200","Id-201",new  BigDecimal(500));
		try {
			Mockito.when(service.transferAmount(transferAccountRequest)).thenReturn(transferAccountRequest);

			mockMvc.perform(post("/v1/transferAmounts/transfer").contentType(MediaType.APPLICATION_JSON)
					.content("{\"fromAccountNumber\": \"Id-200\",\"toAccountNumber\": \"Id-201\",\"amount\": 500}")).andExpect(status().isBadRequest());
		} catch (TransactionExceptionMessage ex) {
			assertThat(ex.getMessage()).isEqualTo("Transaction was not Sucessfull");
		}
	}

}		
