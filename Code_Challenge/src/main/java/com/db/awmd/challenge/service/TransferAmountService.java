package com.db.awmd.challenge.service;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.domain.TransferAccountRequest;

@Service
public interface TransferAmountService {

	public Object transferAmount(TransferAccountRequest transferAccountRequest);
}

