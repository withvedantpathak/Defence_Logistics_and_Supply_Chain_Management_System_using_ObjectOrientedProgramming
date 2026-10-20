# Syncing the cloud workdir to the folder on the user's PC
The PC shell has only a JRE (no javac), so building happens in the cloud workdir `/home/claude/dlscms`.
At the end of each phase:
1. Cloud: `cd /home/claude/dlscms && mkdir -p /mnt/user-data/outputs && rm -f /mnt/user-data/outputs/dlscms_snapshot.zip && zip -qr /mnt/user-data/outputs/dlscms_snapshot.zip . -x 'out/*' 'dist/*' '.git/*'`
2. Claude tool `device_commit_files`: `stagedPath=/mnt/user-data/outputs/dlscms_snapshot.zip` -> `devicePath=C:\Vedant\NMIMS\projects\OOP\_sync\dlscms_snapshot.zip`
3. Claude tool `device_bash`: `cd "$HOME/mnt/OOP" && unzip -oq _sync/dlscms_snapshot.zip -d dlscms`
4. Also commit the built `dist/dlscms.jar` the same way (-> `dlscms/dist/dlscms.jar`) so the human can run it with a JRE.
Note: `unzip -o` overwrites but never deletes; if files were removed in the cloud, delete them on the device explicitly.
