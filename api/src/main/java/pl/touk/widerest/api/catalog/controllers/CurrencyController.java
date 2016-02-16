package pl.touk.widerest.api.catalog.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.catalog.exceptions.CurrencyNotFoundException;
import pl.touk.widerest.security.config.ResourceServerConfig;

import javax.annotation.Resource;
import java.util.Optional;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/currency")
@Api(value = "currency", description = "Currency endpoint")
public class CurrencyController {

    @Resource (name = "blCurrencyService")
    BroadleafCurrencyService currencyService;

    @PreAuthorize("hasRole('PERMISSION_ALL_SYSTEM_PROPERTY')")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation("Get current default currency")
    @ResponseBody
    public BroadleafCurrency getDefault(){
        return currencyService.findDefaultBroadleafCurrency();
    }

    @PreAuthorize("hasRole('PERMISSION_ALL_SYSTEM_PROPERTY')")
    @Transactional
    @RequestMapping(method = RequestMethod.PUT)
    @ApiOperation("Set default currency")
    public BroadleafCurrency setDefault(@ApiParam @RequestBody String currency) throws CurrencyNotFoundException {
        return Optional.of(currencyService.findCurrencyByCode(currency.toUpperCase()))
                .map(newDefaultCurrency -> {
                    final BroadleafCurrency previousDefault = currencyService.findDefaultBroadleafCurrency();

                    previousDefault.setDefaultFlag(false);
                    newDefaultCurrency.setDefaultFlag(true);

                    currencyService.save(previousDefault);
                    currencyService.save(newDefaultCurrency);

                    return newDefaultCurrency;
                })
                .orElseThrow(() -> new CurrencyNotFoundException("Could not find currency in database"));
    }
}
