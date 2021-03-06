package com.axelor.apps.supplychain.service;

import com.axelor.apps.account.db.FixedAsset;
import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.service.FixedAssetServiceImpl;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.exception.AxelorException;
import com.google.inject.persist.Transactional;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

public class FixedAssetServiceSupplyChainImpl extends FixedAssetServiceImpl {

  @Transactional
  @Override
  public List<FixedAsset> createFixedAssets(Invoice invoice) throws AxelorException {

    List<FixedAsset> fixedAssetList = super.createFixedAssets(invoice);

    if (CollectionUtils.isEmpty(fixedAssetList)) {
      return null;
    }

    StockLocation stockLocation =
        invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getStockLocation() : null;

    for (FixedAsset fixedAsset : fixedAssetList) {

      PurchaseOrderLine pol = fixedAsset.getInvoiceLine().getPurchaseOrderLine();

      fixedAsset.setStockLocation(stockLocation);

      if (fixedAsset.getInvoiceLine().getIncomingStockMove() != null
          || CollectionUtils.isNotEmpty(
              fixedAsset.getInvoiceLine().getIncomingStockMove().getStockMoveLineList())) {
        fixedAsset.setTrackingNumber(
            fixedAsset
                .getInvoiceLine()
                .getIncomingStockMove()
                .getStockMoveLineList()
                .stream()
                .filter(l -> pol.equals(l.getPurchaseOrderLine()))
                .findFirst()
                .map(StockMoveLine::getTrackingNumber)
                .orElse(null));
        fixedAsset.setStockLocation(
            fixedAsset.getInvoiceLine().getIncomingStockMove().getToStockLocation());
      }
    }

    return fixedAssetList;
  }
}
