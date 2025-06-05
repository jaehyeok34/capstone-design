add:
	git worktree add -b api-f api-gateway/ origin/api-gateway
	git worktree add -b -f matching-key-server/ origin/matching-key-server
	git worktree add -b -f data-server/ origin/data-server
	git worktree add -b -f matching-server/ origin/matching-server  
	git worktree add -b -f pseudonymization-server/ origin/pseudonymization-server
	git worktree add -b -f pii-detection-server/ origin/pii-detection-server 

rm:
	git worktree remove $(CURDIR)/api-gateway
	git worktree remove $(CURDIR)/matching-key-server
	git worktree remove $(CURDIR)/data-server
	git worktree remove $(CURDIR)/matching-server
	git worktree remove $(CURDIR)/pseudonymization-server
	git worktree remove $(CURDIR)/pii-detection-server
	
rf:
	git worktree remove --force $(CURDIR)/api-gateway
	git worktree remove --force $(CURDIR)/matching-key-server
	git worktree remove --force $(CURDIR)/data-server
	git worktree remove --force $(CURDIR)/matching-server
	git worktree remove --force $(CURDIR)/pseudonymization-server
	git worktree remove --force $(CURDIR)/pii-detection-server