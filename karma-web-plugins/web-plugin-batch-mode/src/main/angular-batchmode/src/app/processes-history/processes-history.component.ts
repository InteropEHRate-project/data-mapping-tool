import {Component, OnInit} from '@angular/core';
import {BatchModeBackendService} from "../common/batch-mode-backend.service";
import {HistoryRequest} from "../common/request/historyRequest.model";
import {Process} from "../common/process.model";


@Component({
  selector: 'app-processes-history',
  templateUrl: './processes-history.component.html',
  styleUrls: ['./processes-history.component.css']
})
export class ProcessesHistoryComponent implements OnInit {

  displayedColumns: string[] = ['processName', 'rdfURL', 'emlURL', 'completed', 'error', 'message'];
  dataSource: Process[] = [];

  constructor(
    private batchModeBackendService: BatchModeBackendService
  ) {
  }

  ngOnInit(): void {
    this.reloadHistoryPage();
  }

  /**
   * reinitialise history component
   */
  reloadHistoryPage() {
    this.batchModeBackendService.makeGetRequest(new HistoryRequest())
      .subscribe(
        (results: any) => { // on success
          console.debug(results['processes']);
          this.dataSource = results['processes'];
        },
        (err: any) => { // on error
          console.log(`Error while getting History: ${err}`);
        },
        () => { // on completion
          console.log("reload History completed");
        }
      );
  }

}
