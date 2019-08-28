package com.axelor.gst.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.invoice.InvoiceLineService;
import com.axelor.apps.account.service.invoice.factory.CancelFactory;
import com.axelor.apps.account.service.invoice.factory.ValidateFactory;
import com.axelor.apps.account.service.invoice.factory.VentilateFactory;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.apps.base.service.alarm.AlarmEngineService;
import com.axelor.apps.businessproject.service.InvoiceServiceProjectImpl;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GstInvoiceServiceImp extends InvoiceServiceProjectImpl implements GstInvoiceService {

  @Inject
  public GstInvoiceServiceImp(
      ValidateFactory validateFactory,
      VentilateFactory ventilateFactory,
      CancelFactory cancelFactory,
      AlarmEngineService<Invoice> alarmEngineService,
      InvoiceRepository invoiceRepo,
      AppAccountService appAccountService,
      PartnerService partnerService,
      InvoiceLineService invoiceLineService) {
    super(
        validateFactory,
        ventilateFactory,
        cancelFactory,
        alarmEngineService,
        invoiceRepo,
        appAccountService,
        partnerService,
        invoiceLineService);
    // TODO Auto-generated constructor stub
  }

  @Inject
  GstInvoiceLineService gstInvoiceLineService;
  
  @Override
  public Invoice compute(Invoice invoice) throws AxelorException {
    super.compute(invoice);
    BigDecimal netCgst = BigDecimal.ZERO;
    BigDecimal netSgst = BigDecimal.ZERO;
    BigDecimal netIgst = BigDecimal.ZERO;
    for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
      netCgst = netCgst.add(invoiceLine.getCgst());
      netSgst = netSgst.add(invoiceLine.getSgst());
      netIgst = netIgst.add(invoiceLine.getIgst());
    }
    invoice.setNetCgst(netCgst);
    invoice.setNetIgst(netIgst);
    invoice.setNetSgst(netSgst);
    return invoice;
  }

  @Override
  public Invoice setProductItem(Invoice invoice, String idList, int partyId) {
    if (idList != null) {
      Partner partner =
          Beans.get(PartnerRepository.class).all().filter("self.id = ?", partyId).fetchOne();
      invoice.setPartner(partner);

      List<InvoiceLine> invoiceItemList = new ArrayList<InvoiceLine>();
      String[] items =
          idList.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
      long[] results = new long[items.length];
      for (int i = 0; i < items.length; i++) {
        results[i] = Integer.parseInt(items[i]);
        InvoiceLine invoiceLine = new InvoiceLine();
        Product product = Beans.get(ProductRepository.class).find(results[i]);
        invoiceLine.setProductName("[" + product.getCode() + "] " + product.getName());
        invoiceLine.setPrice(product.getSalePrice());
        invoiceLine.setHsbn(product.getHsbn());
        invoiceLine.setProduct(product);
        invoiceLine.setQty(BigDecimal.ONE)	;
        //	invoiceLine = gstInvoiceLineService.fillProductInformation(invoiceLine, invoice);
        invoiceItemList.add(invoiceLine);
      }
      invoice.setInvoiceLineList(invoiceItemList);
    }
    return invoice;
  }
}
