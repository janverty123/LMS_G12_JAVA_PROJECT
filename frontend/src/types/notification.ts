export interface Announcement { id: string; classSectionId: string; title: string; content: string; authorName: string; createdAt: string; updatedAt: string; }
export interface AppNotification { id: string; type: string; message: string; referenceKey: string; read: boolean; createdAt: string; }
