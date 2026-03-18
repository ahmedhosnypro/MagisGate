class LogEntry {
  final DateTime time;
  final String message;

  LogEntry(this.message) : time = DateTime.now();

  String get formatted => '[${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}:${time.second.toString().padLeft(2, '0')}] $message';
}
