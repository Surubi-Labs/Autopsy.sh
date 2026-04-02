.PHONY: test lint typecheck all

all: lint typecheck test

test:
	uv run pytest tests/ -v --tb=short

lint:
	uv run ruff check autopsy/ tests/

typecheck:
	uv run mypy autopsy/
