import {Component, OnInit, Output, EventEmitter} from '@angular/core';
import {Process} from "../common/process.model";
import {BatchModeBackendService} from "../common/batch-mode-backend.service";
import {ActiveProcessRequest} from "../common/request/activeProcessRequest.model";

@Component({
  selector: 'app-processes-active',
  templateUrl: './processes-active.component.html',
  styleUrls: ['./processes-active.component.css']
})
export class ProcessesActiveComponent implements OnInit {

  displayedColumns: string[] = ['processName', 'completed', 'alive', 'message'];
  activeProcesses: Process[] = [];
  private numberOfActiveProcesses: number = 0;
  @Output() refreshHistory = new EventEmitter<boolean>();

  constructor(
    private batchModeBackendService: BatchModeBackendService
  ) {
  }

  ngOnInit(): void {
  }

  onActiveProcessLengthChanges(){
    if(this.numberOfActiveProcesses>=this.activeProcesses.length){
      this.refreshHistory.emit(true);
    }
    this.numberOfActiveProcesses = this.activeProcesses.length;
  }


  /**
   * reinitialise history component
   */
  reloadActivePage() {
    let activeProcessRequest = new ActiveProcessRequest();
    this.batchModeBackendService.makeSeeRequest(activeProcessRequest)
      .subscribe(
        message => { // on success
          this.activeProcesses = message;
          this.onActiveProcessLengthChanges();
        },
        err => { // on error
          console.log(`Active process SSE connection closed by the server`);
          this.activeProcesses = [];
          this.onActiveProcessLengthChanges();
        },
        () => { // on completion
          console.log("Active process SSE connection closed by client completed");
          // on complete, close connection
          this.batchModeBackendService.closeSseConnection(activeProcessRequest);
          this.activeProcesses = [];
          this.onActiveProcessLengthChanges();
        }
      );
  }

}
