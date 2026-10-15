PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS templates (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT NOT NULL,
  filename TEXT NOT NULL,
  docx BLOB NOT NULL
);

CREATE TABLE IF NOT EXISTS template_fields (
  template_id TEXT NOT NULL REFERENCES templates(id),
  field_key TEXT NOT NULL,
  label TEXT NOT NULL,
  required INTEGER NOT NULL CHECK (required IN (0, 1)),
  repeatable INTEGER NOT NULL CHECK (repeatable IN (0, 1)),
  position INTEGER NOT NULL,
  PRIMARY KEY (template_id, field_key)
);

CREATE TABLE IF NOT EXISTS documents (
  id TEXT PRIMARY KEY,
  template_id TEXT NOT NULL REFERENCES templates(id),
  status TEXT NOT NULL CHECK (status IN ('EM_EDICAO', 'FINALIZADO')),
  drive_file_id TEXT,
  edit_url TEXT,
  final_origin TEXT CHECK (final_origin IN ('UPLOAD', 'DRIVE')),
  final_docx BLOB,
  created_at TEXT NOT NULL,
  finalized_at TEXT
);

CREATE TABLE IF NOT EXISTS previews (
  id TEXT PRIMARY KEY,
  document_id TEXT NOT NULL UNIQUE REFERENCES documents(id) ON DELETE CASCADE,
  origin TEXT NOT NULL CHECK (origin IN ('UPLOAD', 'DRIVE')),
  docx BLOB NOT NULL,
  valid INTEGER NOT NULL CHECK (valid IN (0, 1)),
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS preview_metadata (
  preview_id TEXT NOT NULL REFERENCES previews(id) ON DELETE CASCADE,
  field_key TEXT NOT NULL,
  item_position INTEGER NOT NULL,
  value TEXT NOT NULL,
  PRIMARY KEY (preview_id, field_key, item_position)
);

CREATE TABLE IF NOT EXISTS document_metadata (
  document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  field_key TEXT NOT NULL,
  item_position INTEGER NOT NULL,
  value TEXT NOT NULL,
  PRIMARY KEY (document_id, field_key, item_position)
);
