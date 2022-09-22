import {Component, OnInit, ViewChild} from '@angular/core';
import {MatDialog} from "@angular/material/dialog";
import {CreateProcessDialogComponent} from "../create-process-dialog/create-process-dialog.component";
import {RunProcessRequest} from "../common/request/runProcessRequest.model";
import {BatchModeBackendService} from "../common/batch-mode-backend.service";
import {ProcessesHistoryComponent} from "../processes-history/processes-history.component";
import {HttpEventType} from "@angular/common/http";
import {MatSnackBar} from "@angular/material/snack-bar";
import {ProcessesActiveComponent} from "../processes-active/processes-active.component";

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  @ViewChild(ProcessesHistoryComponent)
  private historyComponent: ProcessesHistoryComponent | undefined;

  @ViewChild(ProcessesActiveComponent)
  private activeComponent: ProcessesActiveComponent | undefined;

  selectedIndex = 0;
  progress: number = 0;

  constructor(
    private _snackBar: MatSnackBar,
    public createProcessDialog: MatDialog,
    public infoDialog: MatDialog,
    private batchModeBackendService: BatchModeBackendService,
  ) {
  }

  ngOnInit(): void {
  }

  openCreateProcessDialog() {
    const dialogRef = this.createProcessDialog.open(CreateProcessDialogComponent);

    dialogRef.afterClosed().subscribe((results: any) => {
      if (results instanceof RunProcessRequest) {
        this.runProcessHTTPRequest(results);
        let snackBarRef = this.openSnackBar('Process Started Successfully', 'Show Progress', 8000);
        this.reloadActiveProcessPage();
        snackBarRef.onAction().subscribe(() => {
          this.selectTab(1);
        });
      }
    });
  }

  openSnackBar(message: string, action: string, duration: any) {
    return this._snackBar.open(message, action, {
      duration: duration,
      horizontalPosition: 'right',
      verticalPosition: 'top',
    });
  }

  selectTab(index: number): void {
    this.selectedIndex = index;
  }

  reloadHistoryPage() {
    this.historyComponent?.reloadHistoryPage();
  }

  reloadActiveProcessPage() {
    this.activeComponent?.reloadActivePage();
  }

  private runProcessHTTPRequest(request: RunProcessRequest) {
    this.batchModeBackendService.makePostRequest(request)
      .subscribe((result: any) => {
          if (result.type === HttpEventType.UploadProgress) {
            console.log("upload percentage: "+Math.round((100 * result.loaded) / result.total));
          }
        },
        (err: any) => { // on error
          console.log(`Error while running standalone process: ${err}`);
        },
        () => { // on completion
          console.log("reload History completed");
        }
      );

  }

  onRefreshHistory($event: boolean) {
    let snackBarRef = this.openSnackBar('Process Completed Successfully', 'Show History', 8000);
    this.reloadHistoryPage();
    snackBarRef.onAction().subscribe(() => {
      this.selectTab(0);
    });
  }

  reloadHomePage() {
    this.reloadActiveProcessPage();
    this.reloadHistoryPage();
  }
}
