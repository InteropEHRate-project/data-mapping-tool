import {DatasetsSearchRequest} from './datasetsSearchRequest.model';
import {Filter} from "../filter.model";
import {FiltersEnum} from "../api.enum";

describe('DatasetsSearchRequest', () => {
  let datasetsRequest: DatasetsSearchRequest;

  afterEach(() => {
    datasetsRequest = null;
  });

  /**
   * #getQueryParameters() empty
   */
  it('#getQueryParameters(): empty',
    () => {
      datasetsRequest = new DatasetsSearchRequest();
      expect(datasetsRequest).toBeTruthy();
      const parameters = 'facet.field=["tags","organization","groups","res_format","resource_license_en"]';
      expect(datasetsRequest.getQueryParameters()).toBe(parameters);
    }
  );

  /**
   * #getQueryParameters() one selected filter
   */
  it('#getQueryParameters(): one selected filter',
    () => {
      let filters: Filter[] = [];
      filters.push(new Filter({
        type: FiltersEnum.tags,
        name: 'filter1'
      }));
      datasetsRequest = new DatasetsSearchRequest({
        selectedFilters: filters
      });
      expect(datasetsRequest).toBeTruthy();
      const parameters = 'facet.field=["tags","organization","groups","res_format","resource_license_en"]' +
        '&fq=(tags:"filter1")';
      expect(datasetsRequest.getQueryParameters()).toBe(parameters);
    }
  );

  /**
   * #getQueryParameters() multiple selected filters
   */
  it('#getQueryParameters(): multiple selected filters',
    () => {
      let filters: Filter[] = [];
      filters.push(new Filter({
        type: FiltersEnum.tags,
        name: 'filter1'
      }));
      filters.push(new Filter({
        type: FiltersEnum.tags,
        name: 'filter2'
      }));
      filters.push(new Filter({
        type: FiltersEnum.organization,
        name: 'filter3'
      }));
      filters.push(new Filter({
        type: FiltersEnum.groups,
        name: 'filter4'
      }));
      filters.push(new Filter({
        type: FiltersEnum.res_format,
        name: 'filter5'
      }));
      filters.push(new Filter({
        type: FiltersEnum.resource_license_en,
        name: 'filter6'
      }));
      datasetsRequest = new DatasetsSearchRequest({
        selectedFilters: filters
      });
      expect(datasetsRequest).toBeTruthy();
      const parameters = 'facet.field=["tags","organization","groups","res_format","resource_license_en"]' +
        '&fq=(tags:("filter1""AND""filter2")' +
        '"AND"organization:"filter3"' +
        '"AND"groups:"filter4"' +
        '"AND"res_format:"filter5"' +
        '"AND"resource_license_en:"filter6")';
      expect(datasetsRequest.getQueryParameters()).toBe(parameters);
    }
  );

  /**
   * #getQueryParameters() start position
   */
  it('#getQueryParameters(): start position',
    () => {
      datasetsRequest = new DatasetsSearchRequest({
        start: 10
      });
      expect(datasetsRequest).toBeTruthy();
      const parameters = 'facet.field=["tags","organization","groups","res_format","resource_license_en"]' +
        '&start=10';
      expect(datasetsRequest.getQueryParameters()).toBe(parameters);
    }
  );

  /**
   * #getQueryParameters() number of rows
   */
  it('#getQueryParameters(): number of rows',
    () => {
      datasetsRequest = new DatasetsSearchRequest({
        row: 10
      });
      expect(datasetsRequest).toBeTruthy();
      const parameters = 'facet.field=["tags","organization","groups","res_format","resource_license_en"]' +
        '&rows=10';
      expect(datasetsRequest.getQueryParameters()).toBe(parameters);
    }
  );

  /**
   * #getQueryParameters() free text search
   */
  it('#getQueryParameters(): free text search',
    () => {
      datasetsRequest = new DatasetsSearchRequest({
        freeTextSearch: "free text"
      });
      expect(datasetsRequest).toBeTruthy();
      const parameters = 'facet.field=["tags","organization","groups","res_format","resource_license_en"]' +
        '&q=free text';
      expect(datasetsRequest.getQueryParameters()).toBe(parameters);
    }
  );

});
