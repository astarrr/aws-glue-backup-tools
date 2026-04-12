import com.monovore.decline.*
import cats.syntax.all.*

object GlueBackupTool
    extends CommandApp(
      name = "glue-backup-tool",
      header = "Backup and restore AWS Glue resources",
      main = Opts.subcommand(BackupCommand.command) orElse
        Opts.subcommand(RestoreCommand.command)
    )
