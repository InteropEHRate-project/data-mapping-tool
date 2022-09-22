import {Injectable, Injector} from '@angular/core';
import {HttpClient} from "@angular/common/http";

export enum AppConfigEnum {
  'apiUrl' = 'apiUrl',
  'apiKey' = 'apiKey',
  'datasetId' = 'datasetId',
  'selectedResources' = 'selectedResources',
  'isDatasetSelected' = 'isDatasetSelected'
}

/**
 * Load the property values from assets/app-config.json file.
 *
 * @author Danish
 * @date 2020/05/27
 */
@Injectable()
export class AppConfigService {
  private appConfig;

  constructor(private injector: Injector) {
  }

  /**
   * method to access the property values.
   */
  config() {
    return this.appConfig;
  }

  /**
   * this method will be executed once before the home page loads.
   * loads the config file properties and override them with karma provided values, if any.
   */
  loadAppConfig() {
    let http = this.injector.get(HttpClient);

    return http.get('./assets/app-config.json')
      .toPromise()
      .then(data => {
        // set app config for later use in the application.
        this.appConfig = AppConfigService.overrideIfKarmaConf(data);
      })
      .catch(error => {
        console.warn("Error loading app-config.json, trying karma properties instead");
        let data: { [key: string]: string } = {apiUrl: "", apiKey: ""}
        this.appConfig = AppConfigService.overrideIfKarmaConf(data);
      });
  }

  /**
   * method will override the apiurl and apikey if they are provided by karma GUI.
   *
   * @param data
   */
  private static overrideIfKarmaConf(data: Object) {

    // override apiUrl and apiKey with karma values if exit.
    const newApiUrl = localStorage.getItem(AppConfigEnum.apiUrl);
    const newApiKey = localStorage.getItem(AppConfigEnum.apiKey);
    if (newApiUrl !== 'undefined' && newApiUrl != null) {
      data[AppConfigEnum.apiUrl] = `${window.location.protocol}//${window.location.host}${localStorage.getItem('karmaPathName')}HTTPRequestHandler?${encodeURIComponent(newApiUrl)}`;
      console.log(`New base catalog url selected from karma GUI: ${window.location.protocol}//${window.location.host}${localStorage.getItem('karmaPathName')}HTTPRequestHandler?${encodeURIComponent(newApiUrl)}`);
    }
    if (newApiKey !== 'undefined' && newApiKey != null) {
      data[AppConfigEnum.apiKey] = newApiKey;
      // console.log('New api key selected: ' + newApiKey);
    }
    return data;
  }
}
