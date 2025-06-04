add:
	git worktree add -f api-gateway/ origin/api-gateway
	git worktree add -f matching-key-server/ origin/matching-key-server

remove:
	git worktree remove $(CURDIR)/api-gateway
	git worktree remove $(CURDIR)/matching-key-server
	