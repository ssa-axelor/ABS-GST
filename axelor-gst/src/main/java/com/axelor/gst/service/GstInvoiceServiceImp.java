package com.axelor.gst.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.invoice.InvoiceLineService;
import com.axelor.apps.account.service.invoice.InvoiceServiceImpl;
import com.axelor.apps.account.service.invoice.factory.CancelFactory;
import com.axelor.apps.account.service.invoice.factory.ValidateFactory;
import com.axelor.apps.account.service.invoice.factory.VentilateFactory;
import com.axelor.apps.account.service.invoice.generator.InvoiceGenerator;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.apps.base.service.alarm.AlarmEngineService;
import com.axelor.apps.supplychain.service.invoice.InvoiceServiceSupplychainImpl;
import com.axelor.apps.supplychain.service.invoice.generator.InvoiceGeneratorSupplyChain;
import com.axelor.exception.AxelorException;

import java.math.BigDecimal;

public class GstInvoiceServiceImp extends InvoiceGenerator {


 @Override
	public void computeInvoice(Invoice invoice) throws AxelorException {
		// TODO Auto-generated method stub
		super.computeInvoice(invoice);
		 BigDecimal netAmount = BigDecimal.ZERO;
		    BigDecimal netCgst = BigDecimal.ZERO;
		    BigDecimal netSgst = BigDecimal.ZERO;
		    BigDecimal netIgst = BigDecimal.ZERO;
		    BigDecimal grossAmount = BigDecimal.ZERO;
		    for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
		      netAmount = netAmount.add(invoiceLine.getExTaxTotal());
		      netCgst = netCgst.add(invoiceLine.getCgst());
		      netSgst = netSgst.add(invoiceLine.getSgst());
		      netIgst = netIgst.add(invoiceLine.getIgst());
		    }
		    grossAmount = netAmount.add(netIgst).add(netSgst).add(netCgst);
		    invoice.setNetAmount(netAmount);
		    invoice.setNetCgst(netCgst);
		    invoice.setNetIgst(netIgst);
		    invoice.setNetSgst(netSgst);
		    invoice.setGrossAmount(grossAmount);
		 	
 }

public Invoice generate() throws AxelorException {
	// TODO Auto-generated method stub
	return null;
}

}
